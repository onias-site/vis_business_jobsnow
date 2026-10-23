package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;

/**
 * Last step of {@code VisServiceResume.ChangeStatus}: a record found in either entity (active or inactive) is placed
 * under {@code _entities}; with none, the resume does not exist and the flow ends with {@code NOT_FOUND}.
 */
public class VisBusinessRefuseResumeNotFound implements CcpBusiness {

	/** The key of the resume, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the key of the resume.
	 * @param json the key of the resume, as it came
	 */
	public VisBusinessRefuseResumeNotFound(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Refuses a resume found in no entity.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the same json, when the resume was found
	 * @throws CcpErrorFlowDisturb with {@code NOT_FOUND} when it was not
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		boolean found = jsonAfterTheSteps.containsField(CcpEntity.JsonFieldNames._entities);
		if(found) {
			return jsonAfterTheSteps;
		}
		CcpErrorFlowDisturb notFound = new CcpErrorFlowDisturb(this.json, CcpProcessStatusDefault.NOT_FOUND);
		throw notFound;
	}
}
