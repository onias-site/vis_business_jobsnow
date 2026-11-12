package com.vis.business.bots.linkedin;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of the {@code verifyLinkedinAddress} command, mapped in the {@code stepFlow} of each step: the
 * resume does not exist ({@code resumeNotFound}, the session ends), the answer was not understood ({@code invalidAnswer},
 * the same step asks again), the address was confirmed as invalid ({@code linkedinInvalid}) or as existing
 * ({@code linkedinValid}); both end the session and close the ticket.
 */
public enum VisSupportVerifyLinkedinStatus implements CcpProcessStatus {
	/** Status 404: the resume does not exist. */
	resumeNotFound(404),
	/** Status 400: the answer was not understood. */
	invalidAnswer(400),
	/** Status 200: the operator confirmed that the address is invalid. */
	linkedinInvalid(200),
	/** Status 202: the operator found that the address exists. */
	linkedinValid(202),
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisSupportVerifyLinkedinStatus(int status) {
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
