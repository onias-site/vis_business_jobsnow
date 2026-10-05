package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the skill hierarchy fix suggestion, with their corresponding HTTP codes:
 * {@code userNotAllowed} (403) when the support bot operator chose to ignore the user for the
 * {@code fixSkillHierarchy} command.
 */
public enum VisProcessStatusFixSkillHierarchy implements CcpProcessStatus{
	/** Status 403: the support team chose to ignore the user for the {@code fixSkillHierarchy} command. */
	userNotAllowed(403),
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
