package com.vis.business.compatibility;

import java.util.Set;

/**
 * The facts of the rules between recruiters and resumes loaded whole in memory, for a process that checks thousands of
 * pairs (the daily e-mail of resumes to every recruiter).
 */
public class VisResumeRecruiterFactsInMemory implements VisResumeRecruiterFacts {

	/** The e-mails (hashed) of the recruiters who unsubscribed. */
	private final Set<String> unsubscribedRecruiters;

	/** The e-mails (hashed) of the restricted resumes. */
	private final Set<String> restrictedResumes;

	/** The pairs recruiter + resume of the twin {@code vis_inactive_viewed_resume}: the two hashes joined by the separator. */
	private final Set<String> unwantedPairs;

	/** What joins the two hashes of a pair. */
	private final String separator;

	/**
	 * Keeps the facts.
	 * @param unsubscribedRecruiters the e-mails (hashed) of the recruiters who unsubscribed
	 * @param restrictedResumes the e-mails (hashed) of the restricted resumes
	 * @param unwantedPairs the pairs of the twin of the views, the hash of the recruiter, the separator and the hash of the LinkedIn
	 * @param separator what joins the two hashes of a pair
	 */
	public VisResumeRecruiterFactsInMemory(Set<String> unsubscribedRecruiters, Set<String> restrictedResumes, Set<String> unwantedPairs, String separator) {
		this.unsubscribedRecruiters = unsubscribedRecruiters;
		this.restrictedResumes = restrictedResumes;
		this.unwantedPairs = unwantedPairs;
		this.separator = separator;
	}

	public boolean isUnsubscribed(String recruiterEmailHash) {
		boolean unsubscribed = this.unsubscribedRecruiters.contains(recruiterEmailHash);
		return unsubscribed;
	}

	public boolean isRestricted(String resumeEmailHash) {
		boolean restricted = this.restrictedResumes.contains(resumeEmailHash);
		return restricted;
	}

	public boolean isUnwanted(String recruiterEmailHash, String linkedinHash) {
		String pairKey = recruiterEmailHash + this.separator + linkedinHash;
		boolean unwanted = this.unwantedPairs.contains(pairKey);
		return unwanted;
	}
}
