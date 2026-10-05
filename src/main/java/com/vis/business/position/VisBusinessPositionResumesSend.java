package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * CcpBusiness implementation that represents the step of sending the resumes associated with a position.
 * The implementation is still pending (returns the JSON unchanged).
 */
public class VisBusinessPositionResumesSend implements CcpBusiness{
		

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessPositionResumesSend() {}
	
	/** The single instance. */
	public static final VisBusinessPositionResumesSend INSTANCE = new VisBusinessPositionResumesSend();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
