package com.vis.business.bots.job.posting;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the {@code saveJobPosting} command, mapped in the {@code stepFlow} of its steps: {@code invalidText}
 * asks for the text again, with the reply the step left in {@code botReply}.
 */
public enum VisSupportSaveJobPostingStatus implements CcpProcessStatus {
	/** Status 400: the text does not have from 100 to 5,000 characters. */
	invalidText(400),
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisSupportSaveJobPostingStatus(int status) {
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
