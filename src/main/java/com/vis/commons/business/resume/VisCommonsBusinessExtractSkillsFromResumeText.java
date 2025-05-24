package com.vis.commons.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;

public class VisCommonsBusinessExtractSkillsFromResumeText implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	
	public final static VisCommonsBusinessExtractSkillsFromResumeText INSTANCE = new VisCommonsBusinessExtractSkillsFromResumeText();
	
	private VisCommonsBusinessExtractSkillsFromResumeText() {}
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// TODO Auto-generated method stub
		return null;
	}

}
