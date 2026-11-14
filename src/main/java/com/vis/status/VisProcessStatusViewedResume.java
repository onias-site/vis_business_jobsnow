package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process status of the view of a resume of the e-mail ({@code vis_viewed_resume}), with its HTTP code.
 */
public enum VisProcessStatusViewedResume implements CcpProcessStatus{
	/**
	 * Status 409: the view of the recruiter is in the twin ({@code vis_inactive_viewed_resume}), where the report of its
	 * LinkedIn put it (since 2026-11-13); it is not saved again, so the resume no longer goes to this recruiter.
	 */
	inactiveView(409),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusViewedResume(int status) {
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
