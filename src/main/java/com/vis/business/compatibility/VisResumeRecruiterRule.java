package com.vis.business.compatibility;

import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.entities.VisEntityResume;

/**
 * Every rule that makes a recruiter and a resume incompatible (since 2026-11-28), the only place where they are written.
 * A resume goes to a recruiter (the daily e-mail, the e-mails of the positions, instant or periodic, the e-mail of the
 * interested candidates, the e-mail of a job posting of {@code /salvarVaga}), and a candidate sees and sends their resume to
 * a position, only when no rule is broken ({@link VisResumeRecruiterCompatibility}). A new rule is a new item: it reaches
 * every one of those flows at once, and the compiler asks where else it holds.
 * <p>
 * The recruiter and the resume come as {@link VisResumeRecruiterCompatibility} builds them; what the rules need from the
 * database comes in the {@link VisResumeRecruiterFacts}.
 */
public enum VisResumeRecruiterRule {
	/**
	 * The recruiter unsubscribed from the e-mails of resumes ({@code vis_recruiter_unsubscribed}). Viewing a resume or
	 * saving a position takes them out of there ({@code VisBusinessReactivateRecruiterWhoViewed},
	 * {@code VisBusinessReactivateRecruiterWhoSavedAPosition}).
	 */
	recruiterUnsubscribed {
		public boolean isBroken(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
			String recruiterEmailHash = recruiter.getAsString(VisRecruiterResumesFields.emailHash);
			boolean unsubscribed = facts.isUnsubscribed(recruiterEmailHash);
			return unsubscribed;
		}
		// the recruiter who opens a screen asked to see the resumes, unlike the e-mail they unsubscribed from
		public boolean holdsOnTheScreensOfTheRecruiter() {
			return false;
		}
		public boolean concernsOnlyTheResume() {
			return false;
		}
		public boolean concernsOnlyTheRecruiter() {
			return true;
		}
	},
	/** The resume has a restriction its owner must correct ({@code vis_resume_restriction}). */
	resumeRestricted {
		public boolean isBroken(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
			String resumeEmailHash = resume.getAsString(VisRecruiterResumesFields.emailHash);
			boolean restricted = facts.isRestricted(resumeEmailHash);
			return restricted;
		}
		public boolean holdsOnTheScreensOfTheRecruiter() {
			return true;
		}
		public boolean concernsOnlyTheResume() {
			return true;
		}
		public boolean concernsOnlyTheRecruiter() {
			return false;
		}
	},
	/** The support confirmed that the LinkedIn address of the resume does not open ({@code /verifyLinkedinAddress}). */
	linkedinConfirmedInvalid {
		public boolean isBroken(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
			boolean invalidLinkedin = resume.getAsBoolean(VisEntityResume.Fields.invalidLinkedinAddress);
			return invalidLinkedin;
		}
		public boolean holdsOnTheScreensOfTheRecruiter() {
			return true;
		}
		public boolean concernsOnlyTheResume() {
			return true;
		}
		public boolean concernsOnlyTheRecruiter() {
			return false;
		}
	},
	/** The resume forbids the company of the recruiter ({@code notAllowedCompany}); a personal e-mail has no company. */
	companyForbidden {
		public boolean isBroken(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
			List<String> notAllowedCompanies = resume.getAsStringList(VisEntityResume.Fields.notAllowedCompany);
			boolean forbidden = VisResumeRecruiterCompatibility.isTheCompanyOfTheRecruiterForbidden(recruiter, notAllowedCompanies);
			return forbidden;
		}
		public boolean holdsOnTheScreensOfTheRecruiter() {
			return true;
		}
		public boolean concernsOnlyTheResume() {
			return false;
		}
		public boolean concernsOnlyTheRecruiter() {
			return false;
		}
	},
	/** The recruiter marked the resume as unwanted: the view in the twin {@code vis_inactive_viewed_resume}. */
	resumeUnwantedByTheRecruiter {
		public boolean isBroken(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
			String recruiterEmailHash = recruiter.getAsString(VisRecruiterResumesFields.emailHash);
			String linkedinHash = resume.getAsString(VisRecruiterResumesFields.linkedinHash);
			boolean unwanted = facts.isUnwanted(recruiterEmailHash, linkedinHash);
			return unwanted;
		}
		// the screens show the unwanted resumes apart, and the link opens them without saving the view
		public boolean holdsOnTheScreensOfTheRecruiter() {
			return false;
		}
		public boolean concernsOnlyTheResume() {
			return false;
		}
		public boolean concernsOnlyTheRecruiter() {
			return false;
		}
	},
	;

	/**
	 * Whether the pair breaks this rule.
	 * @param recruiter the recruiter ({@link VisResumeRecruiterCompatibility#getRecruiter(String)}); empty for a rule that
	 * concerns only the resume
	 * @param resume the resume ({@link VisResumeRecruiterCompatibility#getResume(CcpJsonRepresentation)}); empty for a rule
	 * that concerns only the recruiter
	 * @param facts what the rules read from the database
	 * @return {@code true} when the resume must not reach the recruiter
	 */
	public abstract boolean isBroken(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts);

	/**
	 * Whether the rule also holds on the screens the recruiter opens (the resumes of the filters of a position, the link
	 * of the e-mail that opens a resume), and not only on what is sent to them.
	 * @return {@code true} when a resume that breaks it is not shown either
	 */
	public abstract boolean holdsOnTheScreensOfTheRecruiter();

	/**
	 * Whether the rule reads only the resume: one broken blocks the resume for every recruiter (the candidate sees no
	 * position and is told to correct the resume).
	 * @return {@code true} when the recruiter is not read
	 */
	public abstract boolean concernsOnlyTheResume();

	/**
	 * Whether the rule reads only the recruiter: one broken means the recruiter receives no resume at all.
	 * @return {@code true} when the resume is not read
	 */
	public abstract boolean concernsOnlyTheRecruiter();
}
