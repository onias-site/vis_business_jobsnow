package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the new skill suggestion operation, with their corresponding
 * HTTP codes.
 */
public enum VisProcessStatusSuggestNewSkill implements CcpProcessStatus{
	rejectedSkill(420),
	approvedSkill(200),
	pendingSkill(202), 
	alreadyExists(409),
	;
	
	final int status;
	
	private VisProcessStatusSuggestNewSkill(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}

}
