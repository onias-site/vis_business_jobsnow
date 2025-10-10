package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusiness;
import com.vis.entities.VisEntityGroupResumeViewsByResume;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisUtils;

public class VisBusinessGroupResumeViewsByResume implements JnBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessGroupResumeViewsByResume() {}
	
	public static final VisBusinessGroupResumeViewsByResume INSTANCE = new VisBusinessGroupResumeViewsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByResume.ENTITY, 
				VisEntityResumeFreeView.Fields.email, 
				VisEntityResumeFreeView.Fields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
