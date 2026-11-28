package com.vis.business.compatibility;

/**
 * The facts of a resume just saved: its save releases the restriction ({@code VisBusinessReleaseResumeRestriction}, an
 * after update of the same save, which may not have run yet), so the resume saved has none. The other facts come from
 * the facts given.
 */
public class VisResumeRecruiterFactsOfASavedResume implements VisResumeRecruiterFacts {

	/** The facts of the other records. */
	private final VisResumeRecruiterFacts facts;

	/** The e-mail (hashed) of the resume just saved. */
	private final String savedResumeEmailHash;

	/**
	 * Keeps the facts and the resume saved.
	 * @param facts the facts of the other records
	 * @param savedResumeEmailHash the e-mail (hashed) of the resume just saved
	 */
	public VisResumeRecruiterFactsOfASavedResume(VisResumeRecruiterFacts facts, String savedResumeEmailHash) {
		this.facts = facts;
		this.savedResumeEmailHash = savedResumeEmailHash;
	}

	public boolean isUnsubscribed(String recruiterEmailHash) {
		boolean unsubscribed = this.facts.isUnsubscribed(recruiterEmailHash);
		return unsubscribed;
	}

	public boolean isRestricted(String resumeEmailHash) {
		boolean saved = this.savedResumeEmailHash.equals(resumeEmailHash);
		if(saved) {
			return false;
		}
		boolean restricted = this.facts.isRestricted(resumeEmailHash);
		return restricted;
	}

	public boolean isUnwanted(String recruiterEmailHash, String linkedinHash) {
		boolean unwanted = this.facts.isUnwanted(recruiterEmailHash, linkedinHash);
		return unwanted;
	}
}
