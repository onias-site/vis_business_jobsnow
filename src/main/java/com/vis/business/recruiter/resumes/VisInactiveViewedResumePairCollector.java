package com.vis.business.recruiter.resumes;

import java.util.Set;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityViewedResume;

/**
 * Consumer of the records of {@code vis_inactive_viewed_resume} (the twin of {@code vis_viewed_resume}) of
 * {@code VisBusinessSendResumesToRecruiters}: keeps the key of each pair recruiter + resume, so the resume no longer goes
 * to that recruiter.
 */
class VisInactiveViewedResumePairCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the key of each pair recruiter + resume of the twin. */
	private final Set<String> inactivePairs;

	/** Joins the hash of the e-mail of the recruiter and the one of the LinkedIn of the resume in the key of a pair. */
	private final String separator;

	/**
	 * Keeps where the keys go.
	 * @param inactivePairs receives the key of each pair recruiter + resume of the twin
	 * @param separator joins the two hashes in the key of a pair
	 */
	VisInactiveViewedResumePairCollector(Set<String> inactivePairs, String separator) {
		this.inactivePairs = inactivePairs;
		this.separator = separator;
	}

	/**
	 * Keeps the key of the pair of the inactive view.
	 * @param inactiveViewedResume the record of {@code vis_inactive_viewed_resume}
	 */
	public void accept(CcpJsonRepresentation inactiveViewedResume) {
		String emailHash = inactiveViewedResume.getAsString(VisEntityViewedResume.Fields.email);
		String linkedinHash = inactiveViewedResume.getAsString(VisEntityViewedResume.Fields.linkedin);
		String pairKey = emailHash + this.separator + linkedinHash;
		this.inactivePairs.add(pairKey);
	}
}
