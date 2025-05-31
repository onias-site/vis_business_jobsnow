package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.vis.utils.VisUtils;

public class VisBusinessGroupPositionsGroupedByRecruiters implements CcpTopic{

	public static final VisBusinessGroupPositionsGroupedByRecruiters INSTANCE = new VisBusinessGroupPositionsGroupedByRecruiters();
	
	private VisBusinessGroupPositionsGroupedByRecruiters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupPositionsGroupedByRecruiters = VisUtils.groupPositionsGroupedByRecruiters(json);
		return groupPositionsGroupedByRecruiters;
	}

}
