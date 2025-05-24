package com.vis.commons.status;

import com.ccp.process.CcpProcessStatus;

public enum SuggestNewSkillStatus implements CcpProcessStatus{
	rejectedSkill(420),
	approvedSkill(200),
	pendingSkill(202), 
	alreadyExists(409),
	;
	
	final int status;
	
	private SuggestNewSkillStatus(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}

}
