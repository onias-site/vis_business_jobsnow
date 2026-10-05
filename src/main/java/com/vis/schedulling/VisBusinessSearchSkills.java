package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Scheduled task meant to search skills. The implementation is pending: it returns the input JSON
 * without processing it.
 */
public class VisBusinessSearchSkills implements  CcpBusiness{

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessSearchSkills() {}
	
	/** The single instance. */
	public static final VisBusinessSearchSkills INSTANCE = new VisBusinessSearchSkills();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
