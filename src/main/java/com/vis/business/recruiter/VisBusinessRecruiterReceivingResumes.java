package com.vis.business.recruiter;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * CcpBusiness implementation that represents the process of a recruiter receiving resumes.
 * The logic is still pending (returns the input JSON unchanged).
 */
public class VisBusinessRecruiterReceivingResumes implements CcpBusiness{
		
	private VisBusinessRecruiterReceivingResumes() {}
	
	public static final VisBusinessRecruiterReceivingResumes INSTANCE = new VisBusinessRecruiterReceivingResumes();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
