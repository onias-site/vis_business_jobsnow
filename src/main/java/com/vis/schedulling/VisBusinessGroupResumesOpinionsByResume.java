package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusiness;
import com.vis.entities.VisEntityGroupResumesPerceptionsByResume;
import com.vis.entities.VisEntityResumePerception;
import com.vis.utils.VisUtils;

public class VisBusinessGroupResumesOpinionsByResume implements JnBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessGroupResumesOpinionsByResume() {}
	
	public static final VisBusinessGroupResumesOpinionsByResume INSTANCE = new VisBusinessGroupResumesOpinionsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation groupDetailsByMasters = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumePerception.ENTITY, 
				VisEntityGroupResumesPerceptionsByResume.ENTITY, 
				VisEntityResumePerception.Fields.email, 
				VisEntityResumePerception.Fields.timestamp
				);
		
		return groupDetailsByMasters;

	}

}
