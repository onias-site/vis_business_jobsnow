package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntitySkillPending;

/**
 * Action of {@code VisBusinessSkillSuggestionReview} when the suggestion is pending: moves it, with the explanation of
 * the operator, to the entity of the decision.
 */
class VisBusinessTransferSkillSuggestionWithTheDecision implements CcpBusiness {

	/** The input of the review: the suggestion key, the decision and the explanation. */
	private final CcpJsonRepresentation json;

	/** The suggestion key (email + skill), with the readable email. */
	private final CcpJsonRepresentation suggestionKey;

	/**
	 * Keeps the review.
	 * @param json the input of the review: the suggestion key, the decision and the explanation
	 * @param suggestionKey the suggestion key, with the readable email
	 */
	VisBusinessTransferSkillSuggestionWithTheDecision(CcpJsonRepresentation json, CcpJsonRepresentation suggestionKey) {
		this.json = json;
		this.suggestionKey = suggestionKey;
	}

	/**
	 * Transfers the suggestion to the entity of the decision.
	 * @param jsonWithTheStoredSuggestion the json of the procedure, holding the suggestion under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredSuggestion) {
		// the pending entity writes through the messaging, which validates the whole record (description is
		// required); the stored email is the hash, so the readable one from the key prevails
		CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
		CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(this.suggestionKey);
		String explanation = this.json.getAsString(JnJsonCommonsFields.explanation);
		CcpJsonRepresentation suggestionWithExplanation = completeSuggestion.put(JnJsonCommonsFields.explanation, explanation);

		VisSkillSuggestionDecisions decision = this.json.getAsEnum(VisSkillSuggestionReviewFields.decision, VisSkillSuggestionDecisions.class);
		CcpEntity targetEntity = decision.getTargetEntity();
		VisEntitySkillPending.ENTITY.transferDataTo(suggestionWithExplanation, targetEntity);
		return jsonWithTheStoredSuggestion;
	}
}
