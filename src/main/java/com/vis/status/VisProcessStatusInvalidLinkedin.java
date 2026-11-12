package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process status of the link of a resume whose LinkedIn address the support confirmed as invalid
 * ({@code invalidLinkedinAddress}, since 2026-11-12), with its HTTP code.
 */
public enum VisProcessStatusInvalidLinkedin implements CcpProcessStatus{
	/**
	 * Status 410: the LinkedIn address of the resume was confirmed as unreachable; the link no longer opens it, and the
	 * page apologizes to the recruiter.
	 */
	invalidLinkedinAddress(410),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusInvalidLinkedin(int status) {
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
