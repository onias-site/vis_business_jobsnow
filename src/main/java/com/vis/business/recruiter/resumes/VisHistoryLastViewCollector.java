package com.vis.business.recruiter.resumes;

import java.util.Map;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityViewedResume;

/**
 * Consumer of the history rows of {@code vis_viewed_resume} of {@code VisBusinessSendResumesToRecruiters}: keeps the last
 * view of each pair recruiter + resume and of each recruiter.
 */
class VisHistoryLastViewCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the last view of each pair recruiter + resume. */
	private final Map<String, Long> lastViewByPair;

	/** Receives the last view of each recruiter. */
	private final Map<String, Long> lastViewByRecruiter;

	/** Joins the hash of the e-mail of the recruiter and the one of the LinkedIn of the resume in the key of a pair. */
	private final String separator;

	/**
	 * Keeps where the views go.
	 * @param lastViewByPair receives the last view of each pair recruiter + resume
	 * @param lastViewByRecruiter receives the last view of each recruiter
	 * @param separator joins the two hashes in the key of a pair
	 */
	VisHistoryLastViewCollector(Map<String, Long> lastViewByPair, Map<String, Long> lastViewByRecruiter, String separator) {
		this.lastViewByPair = lastViewByPair;
		this.lastViewByRecruiter = lastViewByRecruiter;
		this.separator = separator;
	}

	/**
	 * Updates the last views with the view of the row.
	 * @param historyRow the history row
	 */
	public void accept(CcpJsonRepresentation historyRow) {
		CcpJsonRepresentation record = VisBusinessSendResumesToRecruiters.getRecordOfTheHistoryRow(historyRow);
		String emailHash = record.getAsString(VisEntityViewedResume.Fields.email);
		String linkedinHash = record.getAsString(VisEntityViewedResume.Fields.linkedin);
		Double timestampNumber = historyRow.getAsDoubleNumber(JnJsonCommonsFields.timestamp);
		long timestamp = timestampNumber.longValue();
		String pairKey = emailHash + this.separator + linkedinHash;
		this.lastViewByPair.merge(pairKey, timestamp, Math::max);
		this.lastViewByRecruiter.merge(emailHash, timestamp, Math::max);
	}
}
