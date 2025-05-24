package com.ccp.vis.schedulling;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;

public class VisBusinessGroupSkills implements  Function<CcpJsonRepresentation, CcpJsonRepresentation>{

	private VisBusinessGroupSkills() {}
	
	public static final VisBusinessGroupSkills INSTANCE = new VisBusinessGroupSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
