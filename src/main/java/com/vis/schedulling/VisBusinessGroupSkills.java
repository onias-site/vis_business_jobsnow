package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Scheduled task meant to group skills. The implementation is still pending: it returns the input
 * JSON without processing it.
 */
public class VisBusinessGroupSkills implements  CcpBusiness{

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessGroupSkills() {}
	
	/** The single instance. */
	public static final VisBusinessGroupSkills INSTANCE = new VisBusinessGroupSkills();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
