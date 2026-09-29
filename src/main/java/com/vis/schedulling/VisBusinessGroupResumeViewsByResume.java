package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusinessSendToMensageria;
import com.vis.entities.VisEntityGroupResumeViewsByResume;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Scheduled task that groups the resume views by the candidate's (resume's) e-mail,
 * using VisEntityResumeFreeView as the source and VisEntityGroupResumeViewsByResume as the target.
 * Delegates the logic to the VisUtils.groupDetailsByMasters utility.
 */
public class VisBusinessGroupResumeViewsByResume implements JnBusinessSendToMensageria{
		

	private VisBusinessGroupResumeViewsByResume() {}
	
	public static final VisBusinessGroupResumeViewsByResume INSTANCE = new VisBusinessGroupResumeViewsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupingResult = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByResume.ENTITY, 
				VisJsonCommonsFields.email, 
				JnJsonCommonsFields.timestamp
				);
		
		return groupingResult;
	}

}
