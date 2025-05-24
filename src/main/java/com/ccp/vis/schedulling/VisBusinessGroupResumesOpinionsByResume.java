package com.ccp.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnTopic;
import com.vis.commons.entities.VisEntityGroupResumesPerceptionsByResume;
import com.vis.commons.entities.VisEntityResumePerception;
import com.vis.commons.utils.VisAsyncUtils;

public class VisBusinessGroupResumesOpinionsByResume implements JnTopic{

	private VisBusinessGroupResumesOpinionsByResume() {}
	
	public static final VisBusinessGroupResumesOpinionsByResume INSTANCE = new VisBusinessGroupResumesOpinionsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation groupDetailsByMasters = VisAsyncUtils.groupDetailsByMasters(
				json, 
				VisEntityResumePerception.ENTITY, 
				VisEntityGroupResumesPerceptionsByResume.ENTITY, 
				VisEntityResumePerception.Fields.email, 
				VisEntityResumePerception.Fields.timestamp
				);
		
		return groupDetailsByMasters;

	}

}
