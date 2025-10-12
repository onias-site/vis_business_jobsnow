package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

public class VisBusinessPositionResumesSend implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessPositionResumesSend() {}
	
	public static final VisBusinessPositionResumesSend INSTANCE = new VisBusinessPositionResumesSend();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
