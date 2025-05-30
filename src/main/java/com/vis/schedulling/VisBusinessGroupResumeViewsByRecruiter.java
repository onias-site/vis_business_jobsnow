package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnTopic;
import com.vis.entities.VisEntityGroupResumeViewsByRecruiter;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisAsyncUtils;

public class VisBusinessGroupResumeViewsByRecruiter implements JnTopic{

	private VisBusinessGroupResumeViewsByRecruiter() {}
	
	public static final VisBusinessGroupResumeViewsByRecruiter INSTANCE = new VisBusinessGroupResumeViewsByRecruiter();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisAsyncUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByRecruiter.ENTITY, 
				VisEntityResumeFreeView.Fields.email, 
				VisEntityResumeFreeView.Fields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
