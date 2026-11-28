package com.vis.business.compatibility;

/**
 * What the rules between a recruiter and a resume ({@link VisResumeRecruiterRule}) need from the database, read before
 * the rules run: a process that checks thousands of pairs loads it once in memory
 * ({@link VisResumeRecruiterFactsInMemory}), the others read only the ids of their pairs ({@link VisResumeRecruiterFactsByIds}).
 * A new rule that needs another record adds a method here, and every way of loading must answer it.
 * The e-mails and the LinkedIn addresses come hashed, as the entities keep them.
 */
public interface VisResumeRecruiterFacts {

	/**
	 * Whether the recruiter unsubscribed from the e-mails of resumes ({@code vis_recruiter_unsubscribed}).
	 * @param recruiterEmailHash the e-mail of the recruiter, hashed
	 * @return {@code true} when the recruiter unsubscribed
	 */
	boolean isUnsubscribed(String recruiterEmailHash);

	/**
	 * Whether the resume has a restriction its owner must correct ({@code vis_resume_restriction}).
	 * @param resumeEmailHash the e-mail of the resume, hashed
	 * @return {@code true} when the resume is restricted
	 */
	boolean isRestricted(String resumeEmailHash);

	/**
	 * Whether the recruiter marked the resume as unwanted: the view in the twin {@code vis_inactive_viewed_resume}
	 * ("Negativar CV" or the report of the LinkedIn).
	 * @param recruiterEmailHash the e-mail of the recruiter, hashed
	 * @param linkedinHash the LinkedIn address of the resume, hashed
	 * @return {@code true} when the recruiter does not want the resume
	 */
	boolean isUnwanted(String recruiterEmailHash, String linkedinHash);
}
