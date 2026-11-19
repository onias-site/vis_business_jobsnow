package com.vis.business.bots.recruiter;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the {@code saveRecruiter} command, mapped in the {@code stepFlow} of its steps: {@code askReactivation}
 * goes to the confirmation, {@code invalidAnswer} repeats it and the others end the session; each one with the reply the
 * step left in {@code botReply}.
 */
public enum VisSupportSaveRecruiterStatus implements CcpProcessStatus {
	/** Status 400: the e-mail is not a valid address. */
	invalidEmail(400),
	/** Status 409: the e-mail is already an active recruiter. */
	alreadyRecruiter(409),
	/** Status 202: the e-mail was an inactive recruiter and became active again, with the source {@code support}. */
	reactivated(202),
	/** Status 300: the e-mail is an inactive recruiter; the operator is asked to confirm the reactivation. */
	askReactivation(300),
	/** Status 406: the answer to the confirmation was not understood, the question is asked again. */
	invalidAnswer(406),
	/** Status 204: the operator did not confirm the reactivation, nothing is written. */
	reactivationCancelled(204),
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
