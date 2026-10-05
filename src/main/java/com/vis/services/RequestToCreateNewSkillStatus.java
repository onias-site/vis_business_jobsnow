package com.vis.services;

import com.ccp.process.CcpProcessStatus;

/** Statuses of the request to create a new skill. */
enum RequestToCreateNewSkillStatus implements CcpProcessStatus{
	/** Status 409: the skill already exists. */
	alreadyAdded(409),
	/** Status 412: the skill was rejected before. */
	rejected(412),
	/** Status 409: the skill was approved before. */
	approved(409),
	/** Status 409: the skill is already waiting for review. */
	pending(409),
	/** Status 202: the request was accepted and is waiting for review. */
	analyzing(202)

	
	;
	
	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private RequestToCreateNewSkillStatus(int status) {
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
