package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;

public class VisBusinessExtractSkillsFromResumeText implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	
	public final static VisBusinessExtractSkillsFromResumeText INSTANCE = new VisBusinessExtractSkillsFromResumeText();
	
	private VisBusinessExtractSkillsFromResumeText() {}
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// TODO Auto-generated method stub
		return null;
	}

}
