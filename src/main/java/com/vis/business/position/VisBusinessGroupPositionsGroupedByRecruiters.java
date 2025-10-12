package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.utils.VisUtils;

public class VisBusinessGroupPositionsGroupedByRecruiters implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	public static final VisBusinessGroupPositionsGroupedByRecruiters INSTANCE = new VisBusinessGroupPositionsGroupedByRecruiters();
	
	private VisBusinessGroupPositionsGroupedByRecruiters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupPositionsGroupedByRecruiters = VisUtils.groupPositionsGroupedByRecruiters(json);
		return groupPositionsGroupedByRecruiters;
	}

}
