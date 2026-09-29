package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Scheduled task meant to search skills. The implementation is pending: it returns the input JSON
 * without processing it.
 */
public class VisBusinessSearchSkills implements  CcpBusiness{

	private VisBusinessSearchSkills() {}
	
	public static final VisBusinessSearchSkills INSTANCE = new VisBusinessSearchSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
