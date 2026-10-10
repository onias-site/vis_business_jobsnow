package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntitySkillApproved;
import com.vis.entities.VisEntitySkillReviewed;

/**
 * Callback of the transfer of a {@code VisEntitySkillPending} suggestion to {@link VisEntitySkillApproved}, executed after the
 * transfer has actually happened, that is, when the support bot operator approves the skill. Records the decision in
 * {@link VisEntitySkillReviewed}: from then on, a candidate who suggests the same skill sees this decision and no new
 * ticket reaches the operator.
 */
public class VisBusinessSkillSuggestionShareApproval implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the reviewed suggestion
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisSkillSuggestionDecisions.approved.shareWithEveryone(json);
		return json;
	}
}
