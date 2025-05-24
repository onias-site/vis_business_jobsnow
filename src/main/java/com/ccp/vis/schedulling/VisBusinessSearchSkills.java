package com.ccp.vis.schedulling;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;

public class VisBusinessSearchSkills implements  Function<CcpJsonRepresentation, CcpJsonRepresentation>{

	private VisBusinessSearchSkills() {}
	
	public static final VisBusinessSearchSkills INSTANCE = new VisBusinessSearchSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
