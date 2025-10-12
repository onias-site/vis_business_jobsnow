package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

public class VisBusinessSearchSkills implements  CcpBusiness{

	private VisBusinessSearchSkills() {}
	
	public static final VisBusinessSearchSkills INSTANCE = new VisBusinessSearchSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
