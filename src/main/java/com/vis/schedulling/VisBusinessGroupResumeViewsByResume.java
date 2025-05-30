package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnTopic;
import com.vis.entities.VisEntityGroupResumeViewsByResume;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisAsyncUtils;

public class VisBusinessGroupResumeViewsByResume implements JnTopic{

	private VisBusinessGroupResumeViewsByResume() {}
	
	public static final VisBusinessGroupResumeViewsByResume INSTANCE = new VisBusinessGroupResumeViewsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisAsyncUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByResume.ENTITY, 
				VisEntityResumeFreeView.Fields.email, 
				VisEntityResumeFreeView.Fields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
