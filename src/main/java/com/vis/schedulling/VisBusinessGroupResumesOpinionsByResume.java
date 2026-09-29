package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusinessSendToMensageria;
import com.vis.entities.VisEntityGroupResumesPerceptionsByResume;
import com.vis.entities.VisEntityResumePerception;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Scheduled task that groups the resume perceptions/evaluations by the candidate's e-mail, using
 * VisEntityResumePerception as the source and VisEntityGroupResumesPerceptionsByResume as the target.
 * Delegates to the VisUtils.groupDetailsByMasters utility.
 */
public class VisBusinessGroupResumesOpinionsByResume implements JnBusinessSendToMensageria{
		

	private VisBusinessGroupResumesOpinionsByResume() {}
	
	public static final VisBusinessGroupResumesOpinionsByResume INSTANCE = new VisBusinessGroupResumesOpinionsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation groupingResult = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumePerception.ENTITY, 
				VisEntityGroupResumesPerceptionsByResume.ENTITY, 
				VisJsonCommonsFields.email, 
				JnJsonCommonsFields.timestamp
				);
		
		return groupingResult;

	}

}
