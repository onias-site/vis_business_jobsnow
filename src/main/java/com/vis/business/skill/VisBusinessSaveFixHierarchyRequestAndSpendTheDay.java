package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntityOperationType;
import com.vis.entities.VisEntitySkillFixHierarchyDailyLimit;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Action of {@code VisServiceSkillFixHierarchy.FixSkillHierarchy} once every check passed: saves the request as pending
 * and spends the day of the candidate in {@link VisEntitySkillFixHierarchyDailyLimit}.
 */
public class VisBusinessSaveFixHierarchyRequestAndSpendTheDay implements CcpBusiness {

	/** The request, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the request.
	 * @param json the request, as it came
	 */
	public VisBusinessSaveFixHierarchyRequestAndSpendTheDay(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Saves the request and the daily limit.
	 * @param request the json of the procedure
	 * @return the result of saving the request
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation request) {
		CcpBusiness saveAsPending = CcpEntityOperationType.save.getOperationCallback(VisEntitySkillFixHierarchyPending.ENTITY);
		CcpJsonRepresentation savedRequest = saveAsPending.execute(request);
		// written here, and not by the pending entity, because that one writes through the messaging: the next
		// request of the candidate must already find the day spent
		String email = this.json.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		CcpJsonRepresentation dailyLimit = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkillFixHierarchyDailyLimit.Fields.email, email);
		VisEntitySkillFixHierarchyDailyLimit.ENTITY.save(dailyLimit);
		return savedRequest;
	}
}
