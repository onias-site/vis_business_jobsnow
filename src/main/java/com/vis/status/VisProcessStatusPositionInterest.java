package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process status of the interest of a candidate in a position ({@code vis_position_interest}, since
 * 2026-11-24), with its HTTP code. A position that is not open to the resume (missing, inactive, of another frequency,
 * not satisfied by the resume, of a company the resume forbids or of a recruiter who does not want it) answers 404, as a
 * position that does not exist: the candidate is not told why.
 */
public enum VisProcessStatusPositionInterest implements CcpProcessStatus{
	/** Status 409: the candidate already sent their resume to the position, or it will go in the next full hour. */
	alreadyInterested(409),
	/** Status 410: the validity of the position is over. */
	expiredPosition(410),
	/**
	 * Status 422: the resume has something to correct first, the LinkedIn address confirmed invalid or a restriction
	 * ({@code vis_resume_restriction}); it goes to no recruiter until then.
	 */
	resumeWithPendingIssues(422),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusPositionInterest(int status) {
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
