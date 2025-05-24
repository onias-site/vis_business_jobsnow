package com.vis.commons.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.vis.commons.utils.VisAsyncUtils;

public class VisBusinessGroupPositionsGroupedByRecruiters implements CcpTopic{

	public static final VisBusinessGroupPositionsGroupedByRecruiters INSTANCE = new VisBusinessGroupPositionsGroupedByRecruiters();
	
	private VisBusinessGroupPositionsGroupedByRecruiters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupPositionsGroupedByRecruiters = VisAsyncUtils.groupPositionsGroupedByRecruiters(json);
		return groupPositionsGroupedByRecruiters;
	}

}
