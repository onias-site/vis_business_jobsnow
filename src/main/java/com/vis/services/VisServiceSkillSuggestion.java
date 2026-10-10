package com.vis.services;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.CcpEntityOperationType;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.business.messages.JnBusinessCancelSupportPendingCommand;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntityGroupPositionsBySkills;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.entities.VisEntitySkillReviewed;
import com.vis.messages.VisMessages;
import com.vis.status.VisProcessStatusSuggestSkill;

/**
 * Service for skill suggestions: the candidate suggests a skill that is in the text of their resume and that was not
 * listed, with its synonyms and why. The suggestion stays pending in {@link VisEntitySkillPending} until the support
 * bot operator reviews it, when it moves to {@code VisEntitySkillApproved} or {@code VisEntitySkillRejected}.
 */
public enum VisServiceSkillSuggestion implements JnService {

	/**
	 * Saves the suggestion as pending, which notifies the candidate and the support bot operator. A user that the
	 * operator chose to ignore (in any command: the ignoring is global, {@link VisEntityCommandNotAllowedToUser})
	 * gets {@code userNotAllowed} (403): the suggestion is not saved and nobody is notified. A word already known,
	 * as a skill or as a synonym of one ({@link VisEntityGroupPositionsBySkills}, {@link VisEntitySkill}), gets
	 * {@code skillAlreadyExists} (412), a skill this candidate already had rejected gets {@code alreadyRejected} (410: the
	 * rejection is final), a skill the support already reviewed from another candidate's suggestion gets
	 * {@code alreadyReviewed} (410: the decision holds for everyone, so no new ticket reaches the operator), and a
	 * suggestion of the same skill by the same candidate still pending gets 409.
	 */
	SuggestSkill{
		/**
		 * Runs the checks and saves the suggestion as pending.
		 * @param json the suggestion
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			// a synonym of a known skill (WEM, synonym of VCM) is not a skill by its own name, so the lookup by
			// the id of VisEntitySkill does not see it: the group of its first two letters, read by id, does
			String skill = json.getAsString(VisEntitySkillPending.Fields.skill);
			int wordStatus = VisEntityGroupPositionsBySkills.getWordStatus(skill);
			boolean isKnownWord = wordStatus == 0;

			if(isKnownWord) {
				VisProcessStatusSuggestSkill.skillAlreadyExists.throwException(json);
			}

			CcpBusiness saveAsPending = CcpEntityOperationType.save.getOperationCallback(VisEntitySkillPending.ENTITY);

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntityCommandNotAllowedToUser.ENTITY).returnStatus(VisProcessStatusSuggestSkill.userNotAllowed).and()
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).returnStatus(VisProcessStatusSuggestSkill.skillAlreadyExists).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY).returnStatus(VisProcessStatusSuggestSkill.alreadyRejected).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillReviewed.ENTITY).returnStatus(VisProcessStatusSuggestSkill.alreadyReviewed).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(CcpProcessStatusDefault.CONFLICT).and()
				.executeAction(saveAsPending)
				.andFinallyReturningTheseFields(VisSkillSuggestionResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			return json;
		}
	},

	/**
	 * Returns the candidate's suggestion of the given skill, plus the {@code status} field ({@code pending},
	 * {@code approved} or {@code rejected}). Looks in the pending entity first: a skill suggested again after being
	 * rejected becomes pending again, and that is the one the candidate must see. Without a suggestion of their own,
	 * returns the decision the support took on the same skill suggested by another candidate
	 * ({@link VisEntitySkillReviewed}: skill, synonyms, {@code status} and the operator's {@code explanation}, never
	 * the other candidate's email or justification). Without either, returns an empty json.
	 *
	 * The four entities are queried in a single database round trip (union all); the priority order is applied
	 * afterwards, on the result already in memory.
	 */
	GetSkillSuggestion{
		/**
		 * Searches the suggestion.
		 * @param json the suggestion key
		 * @return the suggestion plus {@code status}, or an empty JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisSkillSuggestionStatus[] statuses = VisSkillSuggestionStatus.values();
			Stream<VisSkillSuggestionStatus> statusesStream = Arrays.stream(statuses);
			Stream<CcpEntity> suggestionEntities = statusesStream.map(status -> status.entity);
			Stream<CcpEntity> reviewedEntity = Stream.of(VisEntitySkillReviewed.ENTITY);
			Stream<CcpEntity> allEntities = Stream.concat(suggestionEntities, reviewedEntity);
			CcpEntity[] entities = allEntities.toArray(CcpEntity[]::new);

			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			CcpSelectUnionAll unionAll = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, entities);
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();

			for (VisSkillSuggestionStatus status : statuses) {
				CcpJsonRepresentation found = status.entity.getRecordFromUnionAll(unionAll, jsonSupplier);
				boolean notFound = found.isEmpty();
				if(notFound) {
					continue;
				}
				CcpJsonRepresentation suggestionWithStatus = found.put(VisSkillSuggestionResponse.status, status.name());
				return suggestionWithStatus;
			}
			// the stored status is the operator's decision, already one of the values of the status field; empty when
			// nobody suggested the skill yet
			CcpJsonRepresentation reviewedFromAnotherSuggestion = VisEntitySkillReviewed.ENTITY.getRecordFromUnionAll(unionAll, jsonSupplier);
			return reviewedFromAnotherSuggestion;
		}
	},

	/**
	 * The candidate withdraws a suggestion that is still pending. Only deletes from the pending entity: what was
	 * already reviewed is review history and cannot be undone by the candidate. Responds 404 when there is no pending
	 * suggestion (e.g. it was reviewed between the candidate opening the modal and giving up).
	 *
	 * The existence is checked before deleting, and not by the result of the deletion: the pending entity writes
	 * through the messaging, so {@code delete} only tells that the message was accepted.
	 */
	DeleteSkillSuggestion{
		/**
		 * Deletes the pending suggestion, or answers 404.
		 * @param json the suggestion key
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpBusiness deleteTheStoredSuggestion = jsonWithTheStoredSuggestion -> {
				CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
				CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(json);
				VisEntitySkillPending.ENTITY.delete(completeSuggestion);
				// the operator's ticket of this suggestion leaves /pendingTickets
				String noticeTemplateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class.getName();
				JnBusinessCancelSupportPendingCommand.INSTANCE.cancel(noticeTemplateId, completeSuggestion);
				return jsonWithTheStoredSuggestion;
			};

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsNotPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).executeAction(deleteTheStoredSuggestion)
				.andFinallyReturningTheseFields(VisSkillSuggestionResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			return json;
		}
	}
	;
}
