package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.vis.entities.VisEntityGroupResumesPerceptionsByRecruiter;
import com.vis.entities.VisEntityResumePerception;
import com.vis.utils.VisUtils;

public class VisBusinessGroupResumesOpinionsByRecruiter implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessGroupResumesOpinionsByRecruiter() {}
	
	public static final VisBusinessGroupResumesOpinionsByRecruiter INSTANCE = new VisBusinessGroupResumesOpinionsByRecruiter();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumePerception.ENTITY, 
				VisEntityGroupResumesPerceptionsByRecruiter.ENTITY, 
				VisEntityResumePerception.Fields.recruiter, 
				VisEntityResumePerception.Fields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
