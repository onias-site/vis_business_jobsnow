package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the skill suggestion, with their corresponding HTTP codes:
 * {@code userNotAllowed} (403) when the support bot operator chose to ignore the user (in any command,
 * the ignoring is global), {@code skillAlreadyExists} (412) when the suggested skill is already
 * known by the system, {@code alreadyRejected} (410) when the support already rejected it for the same user, and
 * {@code alreadyReviewed} (410) when the support already approved or rejected it for another user.
 */
public enum VisProcessStatusSuggestSkill implements CcpProcessStatus{
	/** Status 403: the support team chose to ignore the user. */
	userNotAllowed(403),
	/** Status 412: the skill is already in {@code VisEntitySkill}. */
	skillAlreadyExists(412),
	/**
	 * Status 410: the support already rejected this skill suggested by this user; the rejection is final, as an item
	 * already decided in a skill hierarchy fix is not asked again.
	 */
	alreadyRejected(410),
	/**
	 * Status 410: the support already reviewed this skill, suggested by another user. The decision holds for everyone,
	 * so the suggestion does not reach the operator again: the user gets the decision when looking up the suggestion.
	 */
	alreadyReviewed(410),
	;

	/** The HTTP status code. */
	final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusSuggestSkill(int status) {
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
