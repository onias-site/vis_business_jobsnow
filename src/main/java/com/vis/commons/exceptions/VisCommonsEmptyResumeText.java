package com.vis.commons.exceptions;

import com.ccp.decorators.CcpJsonRepresentation;

@SuppressWarnings("serial")
public class VisCommonsEmptyResumeText extends RuntimeException {

	public final CcpJsonRepresentation resume;

	public VisCommonsEmptyResumeText(CcpJsonRepresentation resume) {
		this.resume = resume;
	}
	
	
	
}
