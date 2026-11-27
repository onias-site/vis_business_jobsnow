package com.vis.business.recruiter.resumes;

import java.util.Set;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.JnEntityDisposableRecord;
import com.vis.entities.VisEntityRecruiterResumes;

/**
 * Consumer of the copies, in {@code jn_disposable_record}, of the lists of {@code vis_recruiter_resumes} written today
 * ({@code VisBusinessSendResumesToRecruiters.getRecipientsOfToday}): keeps the hash of the e-mail of the recruiter of each
 * copy, read from its {@code json} (the record as stored, with the e-mail already hashed by the transformers; the
 * {@code id} of a copy read by a query is the {@code _id} of the document).
 */
class VisRecipientOfTodayCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the hashes of the e-mails. */
	private final Set<String> recipientsOfToday;

	/**
	 * Keeps where the hashes go.
	 * @param recipientsOfToday receives the hashes of the e-mails
	 */
	VisRecipientOfTodayCollector(Set<String> recipientsOfToday) {
		this.recipientsOfToday = recipientsOfToday;
	}

	/**
	 * Keeps the hash of the e-mail of the copy.
	 * @param disposableCopy the copy of a list of {@code vis_recruiter_resumes}
	 */
	public void accept(CcpJsonRepresentation disposableCopy) {
		CcpJsonRepresentation storedRecord = disposableCopy.getInnerJson(JnEntityDisposableRecord.Fields.json);
		String emailHash = storedRecord.getAsString(VisEntityRecruiterResumes.Fields.email);
		this.recipientsOfToday.add(emailHash);
	}
}
