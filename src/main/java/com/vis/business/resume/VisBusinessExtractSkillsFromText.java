package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;

public class VisBusinessExtractSkillsFromText implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	
	public final static VisBusinessExtractSkillsFromText INSTANCE = new VisBusinessExtractSkillsFromText();
	
	private VisBusinessExtractSkillsFromText() {}
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// TODO Auto-generated method stub
		return null;
	}

}
