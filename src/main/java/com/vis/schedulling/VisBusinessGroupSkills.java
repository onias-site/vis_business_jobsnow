package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;

public class VisBusinessGroupSkills implements  CcpBusiness{

	private VisBusinessGroupSkills() {}
	
	public static final VisBusinessGroupSkills INSTANCE = new VisBusinessGroupSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
