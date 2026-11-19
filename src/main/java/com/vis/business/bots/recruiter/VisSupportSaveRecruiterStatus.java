package com.vis.business.bots.recruiter;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the {@code saveRecruiter} command, mapped in the {@code stepFlow} of its steps: {@code askReactivation}
 * and {@code askInactivation} go to their confirmations, {@code invalidAnswer} repeats it and the others end the session; each one with the reply the
 * step left in {@code botReply}.
 */
public enum VisSupportSaveRecruiterStatus implements CcpProcessStatus {
	/** Status 400: the e-mail is not a valid address. */
	invalidEmail(400),
	/** Status 409: the e-mail became an active recruiter between the question of the reactivation and the answer. */
	alreadyRecruiter(409),
	/** Status 202: the e-mail was an inactive recruiter and became active again, with the source {@code support}. */
	reactivated(202),
	/** Status 300: the e-mail is an inactive recruiter; the operator is asked to confirm the reactivation. */
	askReactivation(300),
	/** Status 406: the answer to the confirmation was not understood, the question is asked again. */
	invalidAnswer(406),
	/** Status 204: the operator did not confirm the reactivation, nothing is written. */
	reactivationCancelled(204),
	/** Status 303: the e-mail is an active recruiter; the operator is asked whether to inactivate them. */
	askInactivation(303),
	/** Status 200: the recruiter was inactivated and recorded as unsubscribed. */
	inactivated(200),
	/** Status 205: the operator did not confirm the inactivation, nothing is written. */
	inactivationCancelled(205),
	/** Status 410: the recruiter stopped being active before the confirmation, nothing is written. */
	noLongerActive(410),
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
