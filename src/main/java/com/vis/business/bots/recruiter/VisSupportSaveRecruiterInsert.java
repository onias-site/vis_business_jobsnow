package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityRecruiter;

/** Last step of the flow of {@code /saveRecruiter}: the e-mail is in neither side, so the recruiter is new. */
class VisSupportSaveRecruiterInsert implements CcpBusiness {

	/** The recruiter to save, with the source {@code support}. */
	private final CcpJsonRepresentation recruiter;

	/**
	 * Keeps the recruiter to save.
	 * @param recruiter the recruiter to save, with the source {@code support}
	 */
	VisSupportSaveRecruiterInsert(CcpJsonRepresentation recruiter) {
		this.recruiter = recruiter;
	}

	/**
	 * Saves the recruiter.
	 * @param json the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisEntityRecruiter.ENTITY.save(this.recruiter);
		return json;
	}
}
