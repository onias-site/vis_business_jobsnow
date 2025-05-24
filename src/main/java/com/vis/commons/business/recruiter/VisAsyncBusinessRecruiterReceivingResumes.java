package com.vis.commons.business.recruiter;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpTopic;

public class VisAsyncBusinessRecruiterReceivingResumes implements CcpTopic{

	private VisAsyncBusinessRecruiterReceivingResumes() {}
	
	public static final VisAsyncBusinessRecruiterReceivingResumes INSTANCE = new VisAsyncBusinessRecruiterReceivingResumes();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
