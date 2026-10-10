package com.vis.services;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.CcpEntityOperationType;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.business.messages.JnBusinessCancelSupportPendingCommand;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.skill.VisBusinessSkillFixHierarchyRefuseAlreadyReviewed;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.messages.VisMessages;
import com.vis.status.VisProcessStatusFixSkillHierarchy;

/**
 * Service for skill hierarchy fix suggestions: the candidate asks to associate ({@code add})
 * or dissociate ({@code remove}) skills of their resume with an implicit knowledge ({@code parent}).
 * The suggestion stays pending in {@link VisEntitySkillFixHierarchyPending} until the support bot operator reviews it, when it moves to {@code VisEntitySkillFixHierarchyFulfiled}.
 */
public enum VisServiceSkillFixHierarchy implements JnService {

	/**
	 * Saves the suggestion as pending, which notifies the user and the support bot operator. A user that the
	 * operator chose to ignore (in any command: the ignoring is global, {@link VisEntityCommandNotAllowedToUser})
	 * gets {@code userNotAllowed} (403): the suggestion is not saved and nobody is notified. A request whose skills were
	 * all reviewed before gets {@code alreadyReviewed} (208): nothing stays pending and the user is emailed.
	 */
	FixSkillHierarchy{
		/**
		 * Runs the checks and saves the suggestion as pending; an existing pending suggestion gives 409.
		 * @param json the suggestion
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpBusiness saveAsPending = CcpEntityOperationType.save.getOperationCallback(VisEntitySkillFixHierarchyPending.ENTITY);

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntityCommandNotAllowedToUser.ENTITY).returnStatus(VisProcessStatusFixSkillHierarchy.userNotAllowed).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillFixHierarchyPending.ENTITY).returnStatus(CcpProcessStatusDefault.CONFLICT).and()
				.executeAction(saveAsPending)
				.andFinallyReturningTheseFields(FixSkillHierarchyResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			// the save goes through the messaging, where the refusal of a request with nothing left to decide only
			// emails the user: the answer must tell them too, instead of a 200 that reads as "sent for review"
			boolean alreadyReviewed = VisBusinessSkillFixHierarchyRefuseAlreadyReviewed.allItemsAlreadyReviewed(json);

			if(alreadyReviewed) {
				VisProcessStatusFixSkillHierarchy.alreadyReviewed.throwException(json);
			}

			return json;
		}
	},

	/**
	 * Returns the candidate's suggestion for the given parent and type, plus the {@code status} field.
	 * Looks in the pending entity first: a suggestion resent after being reviewed becomes
	 * pending again, and that is the one the candidate must see. Without a suggestion, returns an empty json.
	 *
	 * The two entities are queried in a single database round trip (union all); the priority order is
	 * applied afterwards, on the result already in memory.
	 */
	GetSkillFixHierarchy{
		/**
		 * Searches the suggestion.
		 * @param json the suggestion key
		 * @return the suggestion plus {@code status}, or an empty JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisSkillFixHierarchyStatus[] statuses = VisSkillFixHierarchyStatus.values();
			Stream<VisSkillFixHierarchyStatus> statusesStream = Arrays.stream(statuses);
			CcpEntity[] entities = statusesStream.map(status -> status.entity).toArray(CcpEntity[]::new);

			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			CcpSelectUnionAll unionAll = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, entities);
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();

			for (VisSkillFixHierarchyStatus status : statuses) {
				CcpJsonRepresentation found = status.entity.getRecordFromUnionAll(unionAll, jsonSupplier);
				boolean notFound = found.isEmpty();
				if(notFound) {
					continue;
				}
				CcpJsonRepresentation suggestionWithStatus = found.put(GetSkillFixHierarchyResponse.status, status.name());
				return suggestionWithStatus;
			}
			return CcpOtherConstants.EMPTY_JSON;
		}
	},

	/**
	 * The candidate withdraws a suggestion that is still pending. Only deletes from the pending entity: what was
	 * already reviewed (fulfiled) is review history and cannot be undone by the candidate. Responds 404 when
	 * there is no pending suggestion (e.g. it was reviewed between the candidate opening the modal and giving up).
	 *
	 * The existence is checked before deleting, and not by the result of the deletion: the pending entity writes
	 * through the messaging, so {@code delete} only tells that the message was accepted, and up to 2026-09-30
	 * the 404 never happened. The stored request goes to the deletion (with the readable email of the key in
	 * place of the stored hash) because its {@code skill} tells which items may have become orphans.
	 */
	DeleteSkillFixHierarchy{
		/**
		 * Deletes the pending suggestion, or answers 404.
		 * @param json the suggestion key
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpBusiness deleteTheStoredRequest = jsonWithTheStoredRequest -> {
				CcpJsonRepresentation storedRequest = jsonWithTheStoredRequest.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillFixHierarchyPending.ENTITY);
				CcpJsonRepresentation completeRequest = storedRequest.mergeWithAnotherJson(json);
				VisEntitySkillFixHierarchyPending.ENTITY.delete(completeRequest);
				// the operator's ticket of this request leaves /pendingTickets
				String noticeTemplateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.class.getName();
				JnBusinessCancelSupportPendingCommand.INSTANCE.cancel(noticeTemplateId, completeRequest);
				return jsonWithTheStoredRequest;
			};

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsNotPresentInEntity(VisEntitySkillFixHierarchyPending.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillFixHierarchyPending.ENTITY).executeAction(deleteTheStoredRequest)
				.andFinallyReturningTheseFields(FixSkillHierarchyResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			return json;
		}
	}
	;
}

/** Fields of the answer of {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy}. */
enum GetSkillFixHierarchyResponse implements CcpJsonFieldName{
	/** The {@code status} field. */
	status
}

/**
 * {@link VisServiceSkillFixHierarchy#FixSkillHierarchy} and {@link VisServiceSkillFixHierarchy#DeleteSkillFixHierarchy}
 * answer with the json they received, so the search procedure returns no field.
 */
enum FixSkillHierarchyResponse implements CcpJsonFieldName{
	/** The {@code inexistentField} field. */
	inexistentField
}

/**
 * Body validation of {@link VisServiceSkillFixHierarchy#FixSkillHierarchy}. Each field copies the
 * rules straight from the class that declares them, because {@code CcpJsonCopyFieldValidationsFrom} is not recursive.
 */
enum FixSkillHierarchy implements CcpJsonFieldName{
	/** The {@code email} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	parent,

	/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, list, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	@CcpJsonFieldValidatorRequired
	skill,

	/** The {@code description} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	description,

	/** The {@code type} field: validated as in {@code VisEntitySkillFixHierarchyPending.Fields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
	@CcpJsonFieldValidatorRequired
	type,
}

/**
 * Body validation of {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy}: only the primary key
 * of the suggestion entities (email + parent + type).
 */
enum GetSkillFixHierarchy implements CcpJsonFieldName{
	/** The {@code email} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	parent,

	/** The {@code type} field: validated as in {@code VisEntitySkillFixHierarchyPending.Fields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
	@CcpJsonFieldValidatorRequired
	type,
}

/**
 * Body validation of {@link VisServiceSkillFixHierarchy#DeleteSkillFixHierarchy}: the same primary
 * key as the lookup (email + parent + type).
 */
enum DeleteSkillFixHierarchy implements CcpJsonFieldName{
	/** The {@code email} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	parent,

	/** The {@code type} field: validated as in {@code VisEntitySkillFixHierarchyPending.Fields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
	@CcpJsonFieldValidatorRequired
	type,
}
