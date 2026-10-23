package com.vis.business.recruiter.resumes;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityRecruiter;

/**
 * Consumer of the recruiters of {@code VisBusinessSendResumesToRecruiters}: sends each recruiter the e-mail with their
 * rows, unless the run is restricted to other recruiters, the recruiter stopped viewing resumes or no resume is left for
 * them.
 */
class VisResumesToTheRecruiterSender implements Consumer<CcpJsonRepresentation> {

	/** The e-mails the run is restricted to; empty for every recruiter. */
	private final List<String> restrictedTo;

	/** The last view of each recruiter, by the hash of their e-mail. */
	private final Map<String, Long> lastViewByRecruiter;

	/** The time before which a last view means the recruiter stopped viewing. */
	private final long viewsSince;

	/** The rows of every resume of the e-mail, in order. */
	private final List<CcpJsonRepresentation> resumesOfTheEmail;

	/** The last view of each pair recruiter + resume. */
	private final Map<String, Long> lastViewByPair;

	/** The alias of each pair recruiter + resume. */
	private final Map<String, String> aliasByPair;

	/** The address of the front end. */
	private final String frontendUrl;

	/** Counts the e-mails sent, in its only position. */
	private final int[] emailsSent;

	/**
	 * Keeps what the e-mail of each recruiter needs.
	 * @param restrictedTo the e-mails the run is restricted to; empty for every recruiter
	 * @param lastViewByRecruiter the last view of each recruiter, by the hash of their e-mail
	 * @param viewsSince the time before which a last view means the recruiter stopped viewing
	 * @param resumesOfTheEmail the rows of every resume of the e-mail, in order
	 * @param lastViewByPair the last view of each pair recruiter + resume
	 * @param aliasByPair the alias of each pair recruiter + resume
	 * @param frontendUrl the address of the front end
	 * @param emailsSent counts the e-mails sent, in its only position
	 */
	VisResumesToTheRecruiterSender(List<String> restrictedTo, Map<String, Long> lastViewByRecruiter, long viewsSince, List<CcpJsonRepresentation> resumesOfTheEmail,
			Map<String, Long> lastViewByPair, Map<String, String> aliasByPair, String frontendUrl, int[] emailsSent) {
		this.restrictedTo = restrictedTo;
		this.lastViewByRecruiter = lastViewByRecruiter;
		this.viewsSince = viewsSince;
		this.resumesOfTheEmail = resumesOfTheEmail;
		this.lastViewByPair = lastViewByPair;
		this.aliasByPair = aliasByPair;
		this.frontendUrl = frontendUrl;
		this.emailsSent = emailsSent;
	}

	/**
	 * Sends the e-mail to the recruiter, when it is due.
	 * @param recruiter the recruiter
	 */
	public void accept(CcpJsonRepresentation recruiter) {
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		boolean outOfThisRun = false == this.restrictedTo.isEmpty() && false == this.restrictedTo.contains(email);
		if(outOfThisRun) {
			return;
		}
		String emailHash = VisBusinessSendResumesToRecruiters.getEmailHash(email);
		Long lastViewOfTheRecruiter = this.lastViewByRecruiter.get(emailHash);
		boolean stoppedViewing = lastViewOfTheRecruiter != null && lastViewOfTheRecruiter < this.viewsSince;
		if(stoppedViewing) {
			return;
		}
		List<CcpJsonRepresentation> rows = VisBusinessSendResumesToRecruiters.getRowsOfTheRecruiter(recruiter, emailHash, this.resumesOfTheEmail, this.lastViewByPair, this.aliasByPair);
		boolean nothingForTheRecruiter = rows.isEmpty();
		if(nothingForTheRecruiter) {
			return;
		}
		VisBusinessSendResumesToRecruiters.sendTheEmail(email, rows, this.frontendUrl);
		this.emailsSent[0]++;
	}
}
