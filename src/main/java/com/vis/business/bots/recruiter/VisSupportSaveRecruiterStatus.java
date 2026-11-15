package com.vis.business.bots.recruiter;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the {@code saveRecruiter} command, mapped in its {@code stepFlow}: each one ends the session with the
 * reply the step left in {@code botReply}.
 */
public enum VisSupportSaveRecruiterStatus implements CcpProcessStatus {
	/** Status 400: the e-mail is not a valid address. */
	invalidEmail(400),
	/** Status 409: the e-mail is already an active recruiter. */
	alreadyRecruiter(409),
	/** Status 202: the e-mail was an inactive recruiter and became active again. */
	reactivated(202),
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisSupportSaveRecruiterStatus(int status) {
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
