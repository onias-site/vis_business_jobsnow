package com.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.CcpEntityOperationType;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.business.messages.JnBusinessCancelSupportPendingCommand;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntityGroupPositionsBySkills;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.entities.VisEntitySkillReviewed;
import com.vis.entities.VisEntitySkillSuggestionWithdrawn;
import com.vis.entities.VisEntitySkillSynonym;
import com.vis.messages.VisMessages;
import com.vis.status.VisProcessStatusDeleteSkillSuggestion;
import com.vis.status.VisProcessStatusSuggestSkill;

/**
 * Service for skill suggestions: the candidate suggests a skill that is in the text of their resume and that was not
 * listed, with its synonyms and why. The suggestion stays pending in {@link VisEntitySkillPending} until the support
 * bot operator reviews it, when it moves to {@code VisEntitySkillApproved} or {@code VisEntitySkillRejected}.
 * Each item takes its input rules from the class of the same name in this package (see {@code JnService}).
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
	 * suggestion of the same skill by the same candidate still pending gets 409. A skill that is a synonym given in a
	 * suggestion this candidate already made ({@link VisEntitySkillSynonym}) gets {@code suggestedThroughSynonym} (409).
	 * <p>
	 * Saving the suggestion also records each of its synonyms in {@link VisEntitySkillSynonym}, pointing to the
	 * suggested skill, in a single bulk.
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
			CcpBusiness saveWithSynonyms = suggestion -> {
				CcpJsonRepresentation ownSuggestionOfTheSynonym = getOwnSuggestionThroughSynonym(json);
				boolean suggestedThroughSynonym = false == ownSuggestionOfTheSynonym.isEmpty();
				if(suggestedThroughSynonym) {
					VisProcessStatusSuggestSkill.suggestedThroughSynonym.throwException(json);
				}
				CcpJsonRepresentation savedSuggestion = saveAsPending.execute(suggestion);
				saveSynonyms(json);
				return savedSuggestion;
			};

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntityCommandNotAllowedToUser.ENTITY).returnStatus(VisProcessStatusSuggestSkill.userNotAllowed).and()
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).returnStatus(VisProcessStatusSuggestSkill.skillAlreadyExists).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY).returnStatus(VisProcessStatusSuggestSkill.alreadyRejected).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillReviewed.ENTITY).returnStatus(VisProcessStatusSuggestSkill.alreadyReviewed).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(CcpProcessStatusDefault.CONFLICT).and()
				.executeAction(saveWithSynonyms)
				.andFinallyReturningTheseFields(VisSkillSuggestionResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			return json;
		}

		/**
		 * The candidate's own suggestion (pending, approved or rejected) of the skill that has the given skill as a
		 * synonym. Two reads by id: the synonym record, then the suggestion of the skill it points to.
		 * @param json the suggestion (email + skill)
		 * @return the suggestion found, or an empty JSON
		 */
		private CcpJsonRepresentation getOwnSuggestionThroughSynonym(CcpJsonRepresentation json) {
			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			CcpSelectUnionAll unionAll = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, VisEntitySkillSynonym.ENTITY);
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();
			CcpJsonRepresentation synonymRecord = VisEntitySkillSynonym.ENTITY.getRecordFromUnionAll(unionAll, jsonSupplier);
			boolean isNotASynonym = synonymRecord.isEmpty();
			if(isNotASynonym) {
				return synonymRecord;
			}
			String suggestedSkill = synonymRecord.getAsString(VisEntitySkillSynonym.Fields.synonym);
			CcpJsonRepresentation jsonWithSuggestedSkill = json.put(VisEntitySkillPending.Fields.skill, suggestedSkill);
			CcpEntity[] suggestionEntities = getSuggestionEntities();
			CcpSelectUnionAll unionAllOfSuggestedSkill = crud.unionAll(jsonWithSuggestedSkill, JnDeleteKeysFromCache.INSTANCE, suggestionEntities);
			CcpJsonRepresentation ownSuggestion = getOwnSuggestion(unionAllOfSuggestedSkill, jsonWithSuggestedSkill);
			return ownSuggestion;
		}

		/**
		 * Records each synonym of the suggestion in {@link VisEntitySkillSynonym}, pointing to the suggested skill, in a
		 * single bulk. A suggestion without synonyms writes nothing.
		 * @param json the suggestion
		 */
		private void saveSynonyms(CcpJsonRepresentation json) {
			String skill = json.getAsString(VisEntitySkillPending.Fields.skill);
			List<String> synonyms = json.getAsStringList(VisEntitySkillPending.Fields.synonym);
			List<CcpBulkItem> synonymItems = new ArrayList<>();
			for (String synonym : synonyms) {
				CcpJsonRepresentation synonymWithItsSkill = CcpOtherConstants.EMPTY_JSON
						.put(VisEntitySkillSynonym.Fields.skill, synonym);
				CcpJsonRepresentation synonymRecord = synonymWithItsSkill
						.put(VisEntitySkillSynonym.Fields.synonym, skill);
				List<CcpBulkItem> bulkItems = VisEntitySkillSynonym.ENTITY.toBulkItems(synonymRecord, CcpBulkEntityOperationType.create);
				synonymItems.addAll(bulkItems);
			}
			JnExecuteBulkOperation.INSTANCE.executeBulk(synonymItems, JnDeleteKeysFromCache.INSTANCE);
		}
	},

	/**
	 * Returns the candidate's suggestion of the given skill, plus the {@code status} field ({@code pending},
	 * {@code approved} or {@code rejected}). Looks in the pending entity first: a skill suggested again after being
	 * rejected becomes pending again, and that is the one the candidate must see. Without a suggestion of their own,
	 * returns the decision the support took on the same skill suggested by another candidate
	 * ({@link VisEntitySkillReviewed}: skill, synonyms, {@code status} and the operator's {@code explanation}, never
	 * the other candidate's email or justification).
	 * <p>
	 * Without either, the skill may be a synonym given in a suggestion ({@link VisEntitySkillSynonym}): typing PTL,
	 * the candidate gets the suggestion of PICK BY LIGHT, suggested with the synonym PTL, plus the {@code typedSynonym}
	 * field with what was typed, so the screen tells that the skill was already suggested through its synonym. Without
	 * any of them, returns an empty json.
	 * <p>
	 * The entities are queried in a single database round trip (union all); the priority order is applied
	 * afterwards, on the result already in memory. Only a skill found as a synonym takes a second round trip.
	 */
	GetSkillSuggestion{
		/**
		 * Searches the suggestion.
		 * @param json the suggestion key
		 * @return the suggestion plus {@code status}, or an empty JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpEntity[] suggestionEntities = getSuggestionEntities();
			Stream<CcpEntity> suggestionEntitiesStream = Arrays.stream(suggestionEntities);
			Stream<CcpEntity> synonymEntity = Stream.of(VisEntitySkillSynonym.ENTITY);
			Stream<CcpEntity> allEntities = Stream.concat(suggestionEntitiesStream, synonymEntity);
			CcpEntity[] entities = allEntities.toArray(CcpEntity[]::new);

			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			CcpSelectUnionAll unionAll = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, entities);

			CcpJsonRepresentation suggestion = getSuggestion(unionAll, json);
			boolean suggestionFound = false == suggestion.isEmpty();
			if(suggestionFound) {
				return suggestion;
			}

			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();
			CcpJsonRepresentation synonymRecord = VisEntitySkillSynonym.ENTITY.getRecordFromUnionAll(unionAll, jsonSupplier);
			boolean isNotASynonym = synonymRecord.isEmpty();
			if(isNotASynonym) {
				return synonymRecord;
			}

			String suggestedSkill = synonymRecord.getAsString(VisEntitySkillSynonym.Fields.synonym);
			CcpJsonRepresentation jsonWithSuggestedSkill = json.put(VisEntitySkillPending.Fields.skill, suggestedSkill);
			CcpSelectUnionAll unionAllOfSuggestedSkill = crud.unionAll(jsonWithSuggestedSkill, JnDeleteKeysFromCache.INSTANCE, suggestionEntities);
			CcpJsonRepresentation suggestionOfSuggestedSkill = getSuggestion(unionAllOfSuggestedSkill, jsonWithSuggestedSkill);
			boolean suggestedSkillNotFound = suggestionOfSuggestedSkill.isEmpty();
			if(suggestedSkillNotFound) {
				return suggestionOfSuggestedSkill;
			}
			String typedSkill = json.getAsString(VisEntitySkillPending.Fields.skill);
			CcpJsonRepresentation suggestionWithTypedSynonym = suggestionOfSuggestedSkill.put(VisSkillSuggestionResponse.typedSynonym, typedSkill);
			return suggestionWithTypedSynonym;
		}

		/**
		 * The candidate's own suggestion of the skill, with its {@code status}; without it, the decision the support
		 * took on the skill suggested by another candidate (its stored status is already one of the values of the status
		 * field); without either, an empty JSON.
		 * @param unionAll the search result, with the suggestion entities
		 * @param json the suggestion key
		 * @return the suggestion plus {@code status}, or an empty JSON
		 */
		private CcpJsonRepresentation getSuggestion(CcpSelectUnionAll unionAll, CcpJsonRepresentation json) {
			CcpJsonRepresentation ownSuggestion = getOwnSuggestion(unionAll, json);
			boolean ownSuggestionFound = false == ownSuggestion.isEmpty();
			if(ownSuggestionFound) {
				return ownSuggestion;
			}
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();
			CcpJsonRepresentation reviewedFromAnotherSuggestion = VisEntitySkillReviewed.ENTITY.getRecordFromUnionAll(unionAll, jsonSupplier);
			return reviewedFromAnotherSuggestion;
		}
	},

	/**
	 * The candidate withdraws a suggestion that is still pending. Only deletes from the pending entity: what was
	 * already reviewed is review history and cannot be undone by the candidate. Responds 404 when there is no pending
	 * suggestion (e.g. it was reviewed between the candidate opening the modal and giving up), and
	 * {@code withdrawnRecently} (429) when the candidate already withdrew a suggestion of the same skill within the hour
	 * ({@link VisEntitySkillSuggestionWithdrawn}, recorded at each withdrawal): suggesting it again is allowed,
	 * withdrawing it again is not, so that nobody opens and cancels tickets to the operator in a loop.
	 *
	 * The existence is checked before deleting, and not by the result of the deletion: the pending entity writes
	 * through the messaging, so {@code delete} only tells that the message was accepted.
	 */
	DeleteSkillSuggestion{
		/**
		 * Deletes the pending suggestion, or answers 404 or 429.
		 * @param json the suggestion key
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpBusiness deleteTheStoredSuggestion = jsonWithTheStoredSuggestion -> {
				CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
				CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(json);
				VisEntitySkillPending.ENTITY.delete(completeSuggestion);
				VisEntitySkillSuggestionWithdrawn.ENTITY.save(json);
				// the operator's ticket of this suggestion leaves /pendingTickets
				String noticeTemplateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class.getName();
				JnBusinessCancelSupportPendingCommand.INSTANCE.cancel(noticeTemplateId, completeSuggestion);
				return jsonWithTheStoredSuggestion;
			};

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsNotPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillSuggestionWithdrawn.ENTITY).returnStatus(VisProcessStatusDeleteSkillSuggestion.withdrawnRecently).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).executeAction(deleteTheStoredSuggestion)
				.andFinallyReturningTheseFields(VisSkillSuggestionResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			return json;
		}
	}
	;

	/**
	 * The entities where the candidate's own suggestion can be, in the order of {@link VisSkillSuggestionStatus}, plus
	 * {@link VisEntitySkillReviewed} (the decision taken on another candidate's suggestion).
	 * @return the entities
	 */
	static CcpEntity[] getSuggestionEntities() {
		VisSkillSuggestionStatus[] statuses = VisSkillSuggestionStatus.values();
		Stream<VisSkillSuggestionStatus> statusesStream = Arrays.stream(statuses);
		Stream<CcpEntity> ownSuggestionEntities = statusesStream.map(status -> status.entity);
		Stream<CcpEntity> reviewedEntity = Stream.of(VisEntitySkillReviewed.ENTITY);
		Stream<CcpEntity> allEntities = Stream.concat(ownSuggestionEntities, reviewedEntity);
		CcpEntity[] entities = allEntities.toArray(CcpEntity[]::new);
		return entities;
	}

	/**
	 * The candidate's own suggestion of the skill plus the {@code status} field, looking in the order of
	 * {@link VisSkillSuggestionStatus}: a skill suggested again after being rejected becomes pending again, and that
	 * is the one the candidate must see.
	 * @param unionAll the search result, with the entities of {@link VisSkillSuggestionStatus}
	 * @param json the suggestion key (email + skill)
	 * @return the suggestion plus {@code status}, or an empty JSON
	 */
	static CcpJsonRepresentation getOwnSuggestion(CcpSelectUnionAll unionAll, CcpJsonRepresentation json) {
		VisSkillSuggestionStatus[] statuses = VisSkillSuggestionStatus.values();
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
		return CcpOtherConstants.EMPTY_JSON;
	}
}
