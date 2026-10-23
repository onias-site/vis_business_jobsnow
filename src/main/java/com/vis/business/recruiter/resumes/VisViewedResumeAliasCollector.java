package com.vis.business.recruiter.resumes;

import java.util.Map;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityViewedResume;

/**
 * Consumer of the records of {@code vis_viewed_resume} of {@code VisBusinessSendResumesToRecruiters}: keeps the alias of
 * each pair recruiter + resume.
 */
class VisViewedResumeAliasCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the alias of each pair recruiter + resume. */
	private final Map<String, String> aliasByPair;

	/** Joins the hash of the e-mail of the recruiter and the one of the LinkedIn of the resume in the key of a pair. */
	private final String separator;

	/**
	 * Keeps where the aliases go.
	 * @param aliasByPair receives the alias of each pair recruiter + resume
	 * @param separator joins the two hashes in the key of a pair
	 */
	VisViewedResumeAliasCollector(Map<String, String> aliasByPair, String separator) {
		this.aliasByPair = aliasByPair;
		this.separator = separator;
	}

	/**
	 * Keeps the alias of the viewed resume.
	 * @param viewedResume the record of {@code vis_viewed_resume}
	 */
	public void accept(CcpJsonRepresentation viewedResume) {
		String emailHash = viewedResume.getAsString(VisEntityViewedResume.Fields.email);
		String linkedinHash = viewedResume.getAsString(VisEntityViewedResume.Fields.linkedin);
		String alias = viewedResume.getAsString(VisEntityViewedResume.Fields.alias);
		String pairKey = emailHash + this.separator + linkedinHash;
		this.aliasByPair.put(pairKey, alias);
	}
}
