package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * The process statuses of the creation and of the edition of a position that are not common to every flow:
 * {@code suppressedEmail} (410) when the e-mail that would receive the resumes is suppressed by the e-mail provider.
 */
public enum VisProcessStatusSavePosition implements CcpProcessStatus{
	/**
	 * Status 410: the {@code resumesEmail} is in {@code JnEntityEmailSuppressed} (permanent bounce, invalid address or
	 * spam report), so the resumes found for the position would never reach it.
	 */
	suppressedEmail(410),
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusSavePosition(int status) {
		this.status = status;
	}

	/**
	 * Returns the HTTP status code.
	 * @return the status code
	 */
	public int asNumber() {
		return status;
	}
}
