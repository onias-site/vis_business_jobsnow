package com.vis.exceptions;

import com.ccp.decorators.CcpJsonRepresentation;

@SuppressWarnings("serial")
public class VisErrorBusinessEmptyResumeText extends RuntimeException {

	public final CcpJsonRepresentation resume;

	public VisErrorBusinessEmptyResumeText(CcpJsonRepresentation resume) {
		this.resume = resume;
	}
	
	
	
}
