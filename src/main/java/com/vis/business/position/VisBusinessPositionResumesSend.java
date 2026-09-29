package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * CcpBusiness implementation that represents the step of sending the resumes associated with a position.
 * The implementation is still pending (returns the JSON unchanged).
 */
public class VisBusinessPositionResumesSend implements CcpBusiness{
		

	private VisBusinessPositionResumesSend() {}
	
	public static final VisBusinessPositionResumesSend INSTANCE = new VisBusinessPositionResumesSend();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
