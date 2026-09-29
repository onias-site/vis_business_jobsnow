package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Scheduled task meant to group skills. The implementation is still pending: it returns the input
 * JSON without processing it.
 */
public class VisBusinessGroupSkills implements  CcpBusiness{

	private VisBusinessGroupSkills() {}
	
	public static final VisBusinessGroupSkills INSTANCE = new VisBusinessGroupSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
