package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityGroupResumesPerceptionsByRecruiter;
import com.vis.entities.VisEntityResumePerception;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Scheduled task that groups the resume perceptions/evaluations by the recruiter's e-mail, using
 * VisEntityResumePerception as the source and VisEntityGroupResumesPerceptionsByRecruiter as the target.
 * Delegates to the VisUtils.groupDetailsByMasters utility.
 */
public class VisBusinessGroupResumesOpinionsByRecruiter implements CcpBusiness{
		

	private VisBusinessGroupResumesOpinionsByRecruiter() {}
	
	public static final VisBusinessGroupResumesOpinionsByRecruiter INSTANCE = new VisBusinessGroupResumesOpinionsByRecruiter();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupingResult = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumePerception.ENTITY, 
				VisEntityGroupResumesPerceptionsByRecruiter.ENTITY, 
				VisJsonCommonsFields.recruiter, 
				JnJsonCommonsFields.timestamp
				);
		
		return groupingResult;
	}

}
