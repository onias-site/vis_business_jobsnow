package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the withdrawal of a skill hierarchy fix request, with their corresponding HTTP codes:
 * {@code withdrawnRecently} (429) when the candidate already withdrew a request of the same parent and type within the hour.
 */
public enum VisProcessStatusDeleteSkillFixHierarchy implements CcpProcessStatus{
	/**
	 * Status 429: the candidate already withdrew a request of this parent and type within the hour
	 * ({@code VisEntitySkillFixHierarchyWithdrawn}); requesting it again is allowed, withdrawing it again is not.
	 */
	withdrawnRecently(429),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusDeleteSkillFixHierarchy(int status) {
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
