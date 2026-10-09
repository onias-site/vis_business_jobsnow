package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the skill hierarchy fix suggestion, with their corresponding HTTP codes:
 * {@code userNotAllowed} (403) when the support bot operator chose to ignore the user (the ignoring is global,
 * it holds for every command), and {@code alreadyReviewed} (208) when every skill of the request was already reviewed.
 */
public enum VisProcessStatusFixSkillHierarchy implements CcpProcessStatus{
	/** Status 403: the support team chose to ignore the user. */
	userNotAllowed(403),
	/**
	 * Status 208: every skill of the request was already reviewed for the same parent and type, so nothing stays
	 * pending; the request is complete and the user is told by email.
	 */
	alreadyReviewed(208),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusFixSkillHierarchy(int status) {
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
