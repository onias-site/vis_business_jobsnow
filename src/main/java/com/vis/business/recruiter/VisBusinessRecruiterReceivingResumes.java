package com.vis.business.recruiter;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

public class VisBusinessRecruiterReceivingResumes implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessRecruiterReceivingResumes() {}
	
	public static final VisBusinessRecruiterReceivingResumes INSTANCE = new VisBusinessRecruiterReceivingResumes();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
