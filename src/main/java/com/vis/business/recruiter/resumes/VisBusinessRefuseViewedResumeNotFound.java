package com.vis.business.recruiter.resumes;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;

/**
 * Last step of the edit and of the moves of a view of the recruiter: a view found on either side of
 * {@code vis_viewed_resume} is placed under {@code _entities}; with none (the group of the month still lists a view no
 * longer stored), the flow ends with {@code NOT_FOUND}.
 */
public class VisBusinessRefuseViewedResumeNotFound implements CcpBusiness {

	/** The input of the request, for the error. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the input of the request.
	 * @param json the input of the request
	 */
	public VisBusinessRefuseViewedResumeNotFound(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Refuses a view found on no side.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the same json, when the view was found
	 * @throws CcpErrorFlowDisturb with {@code NOT_FOUND} when it was not
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		boolean found = jsonAfterTheSteps.containsField(CcpEntity.JsonFieldNames._entities);

		if(found) {
			return jsonAfterTheSteps;
		}

		throw new CcpErrorFlowDisturb(this.json, CcpProcessStatusDefault.NOT_FOUND);
	}
}
