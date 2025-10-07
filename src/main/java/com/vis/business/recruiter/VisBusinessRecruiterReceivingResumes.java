package com.vis.business.recruiter;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpTopic;

public class VisBusinessRecruiterReceivingResumes implements CcpTopic{
	//TODO JSON VALIDATIONS	

	private VisBusinessRecruiterReceivingResumes() {}
	
	public static final VisBusinessRecruiterReceivingResumes INSTANCE = new VisBusinessRecruiterReceivingResumes();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
