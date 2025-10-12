package com.vis.exceptions;

import com.ccp.decorators.CcpJsonRepresentation;

@SuppressWarnings("serial")
public class VisBusinessErrorEmptyResumeText extends RuntimeException {

	public final CcpJsonRepresentation resume;

	public VisBusinessErrorEmptyResumeText(CcpJsonRepresentation resume) {
		this.resume = resume;
	}
	
	
	
}
