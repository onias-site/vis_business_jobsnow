package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the withdrawal of a skill suggestion, with their corresponding HTTP codes:
 * {@code withdrawnRecently} (429) when the candidate already withdrew a suggestion of the same skill within the hour.
 */
public enum VisProcessStatusDeleteSkillSuggestion implements CcpProcessStatus{
	/**
	 * Status 429: the candidate already withdrew a suggestion of this skill within the hour
	 * ({@code VisEntitySkillSuggestionWithdrawn}); suggesting it again is allowed, withdrawing it again is not.
	 */
	withdrawnRecently(429),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusDeleteSkillSuggestion(int status) {
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
