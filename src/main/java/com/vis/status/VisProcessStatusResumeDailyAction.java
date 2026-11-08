package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the actions over a saved resume limited to once a day
 * ({@code VisEntityResumeDailyAction}), with their corresponding HTTP codes.
 */
public enum VisProcessStatusResumeDailyAction implements CcpProcessStatus{
	/**
	 * Status 429: the candidate already ran this action (inactivation, reactivation, exclusion or update) in the last 24
	 * hours; each one emails the candidate, so one of each per day.
	 */
	dailyLimitReached(429),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusResumeDailyAction(int status) {
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
