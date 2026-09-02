package com.vis.services;

import com.ccp.process.CcpProcessStatus;

enum RequestToCreateNewSkillStatus implements CcpProcessStatus{
	alreadyAdded(409),
	rejected(412),
	approved(409),
	pending(409),
	analyzing(202)

	
	;
	
	public final int status;

	private RequestToCreateNewSkillStatus(int status) {
		this.status = status;
	}
	
	public int asNumber() {
		return this.status;
	}
	
}
