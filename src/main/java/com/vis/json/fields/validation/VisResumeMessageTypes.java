package com.vis.json.fields.validation;

/**
 * The kinds of message to the owner of a resume kept in {@code vis_resume_message} (since 2026-11-12), part of its primary
 * key: one message of each kind per chat.
 */
public enum VisResumeMessageTypes {
	/** The support confirmed as invalid the LinkedIn address reported by a recruiter. */
	invalidLinkedinAddress,
	/** A recruiter viewed the resume (not sent yet). */
	newViewInYourResume,
	/** A company of the resume's notAllowedCompany is not valid (not sent yet). */
	invalidNotAllowedCompany,
	;
}
