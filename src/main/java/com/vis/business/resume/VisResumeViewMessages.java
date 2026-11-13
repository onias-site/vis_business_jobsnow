package com.vis.business.resume;

import com.jn.messages.JnSystemMessage;

/**
 * The texts of the message to the owner of a resume about a new view of it (since 2026-11-12), seeded in
 * {@code jn_system_message} by {@code VisEntityResumeMessage}: one for a recruiter of a company, with its domain, and one
 * for a freelancer (an e-mail of a non-professional domain).
 */
public enum VisResumeViewMessages implements JnSystemMessage {
	/** A recruiter of a company: {@code {date}} and {@code {recruiterDomain}}. */
	newViewByCompany,
	/** A freelancer recruiter: {@code {date}}. */
	newViewByFreelancer,
	;
}
