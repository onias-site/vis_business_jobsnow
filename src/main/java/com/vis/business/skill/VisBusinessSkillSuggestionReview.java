package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
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

		CcpBusiness transferWithTheDecision = new VisBusinessTransferSkillSuggestionWithTheDecision(json, suggestionKey);

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
