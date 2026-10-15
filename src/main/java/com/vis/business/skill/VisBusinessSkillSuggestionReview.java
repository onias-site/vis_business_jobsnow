package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkillPending;

/**
 * Applies the decision taken by the support bot operator on a skill suggestion, given the {@code email} and the
 * {@code skill} of the suggestion, the {@code decision} ({@link VisSkillSuggestionDecisions}) and the operator's
 * {@code explanation}: the suggestion leaves {@link VisEntitySkillPending} for {@code VisEntitySkillApproved} or
 * {@code VisEntitySkillRejected}, with the explanation. That transfer is what emails the candidate, and the approval
 * also puts the skill in {@code VisEntitySkill} ({@link VisBusinessSkillSuggestionApply}). A suggestion no longer
 * pending is left alone.
 */
public class VisBusinessSkillSuggestionReview implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSkillSuggestionReview INSTANCE = new VisBusinessSkillSuggestionReview();

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessSkillSuggestionReview() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the suggestion key, the decision and the explanation
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation suggestionKey = json.getJsonPiece(VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill);

		CcpBusiness transferWithTheDecision = jsonWithTheStoredSuggestion -> {
			// the pending entity writes through the messaging, which validates the whole record (description is
			// required); the stored email is the hash, so the readable one from the key prevails
			CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
			CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(suggestionKey);
			String explanation = json.getAsString(JnJsonCommonsFields.explanation);
			CcpJsonRepresentation suggestionWithExplanation = completeSuggestion.put(JnJsonCommonsFields.explanation, explanation);

			VisSkillSuggestionDecisions decision = json.getAsEnum(VisSkillSuggestionReviewFields.decision, VisSkillSuggestionDecisions.class);
			CcpEntity targetEntity = decision.getTargetEntity();
			VisEntitySkillPending.ENTITY.transferDataTo(suggestionWithExplanation, targetEntity);
			return jsonWithTheStoredSuggestion;
		};

		// a single read by id: the presence check and the stored suggestion come together (a suggestion no longer
		// pending runs no action)
		new CcpGetEntityId(suggestionKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).executeAction(transferWithTheDecision)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}

	/** The procedure answers nothing: the business returns the json it received. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field. */
		inexistentField
	}
}
