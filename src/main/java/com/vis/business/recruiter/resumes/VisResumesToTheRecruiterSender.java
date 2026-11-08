package com.vis.business.recruiter.resumes;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterFirstEmail;

/**
 * Consumer of the recruiters of {@code VisBusinessSendResumesToRecruiters}: sends each recruiter the e-mail with their
 * rows, unless the limit of the day was reached, the run is restricted to other recruiters, the address is not valid or
 * no resume is left for them. Only the e-mails sent count for the limit, so a recruiter who is skipped gives their place to
 * the next one. Keeps the last position of each queue (the general one and the one of the legacy views) that had its turn,
 * skipped or not.
 */
class VisResumesToTheRecruiterSender implements Consumer<CcpJsonRepresentation> {

	/** The e-mails the run is restricted to; empty for every recruiter. */
	private final List<String> restrictedTo;

	/** The rows of every resume of the e-mail, in order. */
	private final List<CcpJsonRepresentation> resumesOfTheEmail;

	/** The last view of each pair recruiter + resume. */
	private final Map<String, Long> lastViewByPair;

	/** The alias of each pair recruiter + resume. */
	private final Map<String, String> aliasByPair;

	/** The address of the front end. */
	private final String frontendUrl;

	/** The address of the API of vis. */
	private final String visApiUrl;

	/** Counts the e-mails sent, in its only position. */
	private final int[] emailsSent;

	/** Keeps, in its only position, the last position of the queue that had its turn. */
	private final String[] lastQueuePosition;

	/** Keeps, in its only position, the last position of the queue of the legacy views that had its turn. */
	private final String[] lastLegacyQueuePosition;

	/**
	 * Keeps what the e-mail of each recruiter needs.
	 * @param restrictedTo the e-mails the run is restricted to; empty for every recruiter
	 * @param resumesOfTheEmail the rows of every resume of the e-mail, in order
	 * @param lastViewByPair the last view of each pair recruiter + resume
	 * @param aliasByPair the alias of each pair recruiter + resume
	 * @param frontendUrl the address of the front end
	 * @param visApiUrl the address of the API of vis
	 * @param emailsSent counts the e-mails sent, in its only position
	 * @param lastQueuePosition keeps, in its only position, the last position of the queue that had its turn
	 * @param lastLegacyQueuePosition keeps, in its only position, the last position of the queue of the legacy views that
	 * had its turn
	 */
	VisResumesToTheRecruiterSender(List<String> restrictedTo, List<CcpJsonRepresentation> resumesOfTheEmail, Map<String, Long> lastViewByPair,
			Map<String, String> aliasByPair, String frontendUrl, String visApiUrl, int[] emailsSent, String[] lastQueuePosition, String[] lastLegacyQueuePosition) {
		this.restrictedTo = restrictedTo;
		this.resumesOfTheEmail = resumesOfTheEmail;
		this.lastViewByPair = lastViewByPair;
		this.aliasByPair = aliasByPair;
		this.frontendUrl = frontendUrl;
		this.visApiUrl = visApiUrl;
		this.emailsSent = emailsSent;
		this.lastQueuePosition = lastQueuePosition;
		this.lastLegacyQueuePosition = lastLegacyQueuePosition;
	}

	/**
	 * Sends the e-mail to the recruiter, when it is due.
	 * @param recruiter the recruiter
	 */
	public void accept(CcpJsonRepresentation recruiter) {
		boolean dailyLimitReached = this.emailsSent[0] >= VisBusinessSendResumesToRecruiters.RECRUITERS_PER_DAY;
		if(dailyLimitReached) {
			return;
		}
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		boolean outOfThisRun = false == this.restrictedTo.isEmpty() && false == this.restrictedTo.contains(email);
		if(outOfThisRun) {
			return;
		}
		// who is not in a queue has no position in it: that queue stays where it was
		this.lastQueuePosition[0] = recruiter.getOrDefault(VisRecruiterResumesFields.queuePosition, () -> this.lastQueuePosition[0]);
		this.lastLegacyQueuePosition[0] = recruiter.getOrDefault(VisRecruiterResumesFields.legacyQueuePosition, () -> this.lastLegacyQueuePosition[0]);
		CcpStringDecorator emailText = new CcpStringDecorator(email);
		CcpEmailDecorator emailDecorator = emailText.email();
		boolean invalidAddress = false == emailDecorator.isValid();
		if(invalidAddress) {
			return;
		}
		String emailHash = VisBusinessSendResumesToRecruiters.getEmailHash(email);
		List<CcpJsonRepresentation> rows = VisBusinessSendResumesToRecruiters.getRowsOfTheRecruiter(recruiter, emailHash, this.resumesOfTheEmail, this.lastViewByPair, this.aliasByPair);
		boolean nothingForTheRecruiter = rows.isEmpty();
		if(nothingForTheRecruiter) {
			return;
		}
		VisBusinessSendResumesToRecruiters.sendTheEmail(email, rows, this.frontendUrl, this.visApiUrl);
		this.emailsSent[0]++;
		boolean notTheFirstEmail = false == recruiter.containsField(VisRecruiterResumesFields.firstEmail);
		if(notTheFirstEmail) {
			return;
		}
		CcpJsonRepresentation firstEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterFirstEmail.Fields.email, email);
		VisEntityRecruiterFirstEmail.ENTITY.save(firstEmail);
	}
}
