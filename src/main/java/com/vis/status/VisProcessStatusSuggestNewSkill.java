package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the new skill suggestion operation, with their corresponding
 * HTTP codes.
 */
public enum VisProcessStatusSuggestNewSkill implements CcpProcessStatus{
	/** Status 420: the skill was rejected. */
	rejectedSkill(420),
	/** Status 200: the skill was approved. */
	approvedSkill(200),
	/** Status 202: the skill is waiting for review. */
	pendingSkill(202), 
	/** Status 409: the skill already exists. */
	alreadyExists(409),
	;
	
	/** The HTTP status code. */
	final int status;
	
	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusSuggestNewSkill(int status) {
		this.status = status;
	}

	/**
	 * Returns the HTTP status code.
	 * @return the status code
	 */
	public int asNumber() {
		return this.status;
	}

}
