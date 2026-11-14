package com.vis.business.recruiter.resumes;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * The fields of the table of resumes sent to the recruiters: the columns of each row, the values of the texts of
 * {@link VisRecruiterResumesTexts}, the values of the e-mail and the counters of the sending.
 */
public enum VisRecruiterResumesFields implements CcpJsonFieldName {
	/** The id of the resume in {@code vis_resume}: it identifies the resume in the links, without any personal data. */
	resumeId,
	/** The lines of the column "what matters": salary expectations and experience. */
	whatMatters,
	/** The metropolitan regions of the resume (or the single "home office only"), in the order of the DDD. */
	regions,
	/** The lines of the column "languages and skills". */
	languagesAndSkills,
	/** The lines of the column "role / position / job / occupation". */
	job,
	/** The lines of the column "other information". */
	otherInformation,
	/** The line with the date the resume was added. */
	insertion,
	/** The line with the date of the last change of the resume. */
	lastChange,
	/** The line with the last view of the resume by the recruiter. */
	lastView,
	/** Whether the recruiter already viewed the resume. */
	viewed,
	/** The label of the link that opens the resume: the alias the recruiter gave to it, or "view resume". */
	viewLabel,
	/** The token that signs a link of the e-mail. */
	token,
	/** The alias suggested to the recruiter for a resume never viewed. */
	defaultAlias,
	/** The keys used to sort the resumes. */
	sortKeys,
	/** The rows of the table. */
	resumes,
	/** The table of resumes, as HTML. */
	resumesTable,
	/** The paragraph with the link "see all", empty when every resume fits in the e-mail. */
	seeAll,
	/** The link of the paragraph "see all". */
	seeAllLink,
	/** The link that stops the e-mails to the recruiter. */
	unsubscribeLink,
	/** The address of the front end, where the links of the e-mail go. */
	frontendUrl,
	/** The address of the API of vis, called by the e-mail providers on a one-click unsubscribe. */
	visApiUrl,
	/** The LinkedIn of the person who signs the e-mail. */
	oniasLinkedin,
	/** The skills and their positions in the ranking of {@code vis_skill}. */
	rankings,
	/** The number of resumes of the e-mail. */
	totalOfResumes,
	/**
	 * The items of the column "languages and skills" of a resume, each one with its key, label, group and order, kept in
	 * its row only until the subject of the e-mail is built.
	 */
	subjectItems,
	/** The key of an item of the subject: the same language or skill in two resumes has the same key. */
	itemKey,
	/** The text of an item of the subject, as the column "languages and skills" shows it. */
	itemLabel,
	/** The group of an item of the subject: 0 fluent English or Spanish, 1 skill, 2 other language. */
	itemGroup,
	/** The order of an item of the subject inside its group: the position of the language or the ranking of the skill. */
	itemOrder,
	/** The best ranked skills of all the resumes of the e-mail, joined by commas, for the subject. */
	topSkills,
	/** How many distinct skills of the resumes of the e-mail are not in {@link #topSkills}, for the subject. */
	otherSkills,
	/** A number of a text. */
	count,
	/** The days of a text. */
	days,
	/** The months of a text. */
	months,
	/** The years of a text. */
	years,
	/** The level of a text. */
	level,
	/** The date of a text. */
	date,
	/** How many resumes the e-mail shows. */
	shown,
	/** How many resumes the recruiter has. */
	total,
	/** The e-mails of the recruiters a run is restricted to (support and tests); every recruiter when absent. */
	recruiters,
	/** The SHA-1 of the e-mail of a recruiter, the order of the places of the day. */
	emailHash,
	/** The hash of the LinkedIn of a viewed resume: the key of the details, so the list of the views never carries the LinkedIn. */
	linkedinHash,
	/** The position of a recruiter in the queue of who did not view a resume lately: the digit of the group + the SHA-1 of the e-mail. */
	queuePosition,
	/**
	 * The position of a recruiter in the queue of the legacy views: 13 digits, the greatest moment of 13 digits minus their
	 * last view (the most recent first), + the SHA-1 of the e-mail (signed hexadecimal, of varying length).
	 */
	legacyQueuePosition,
	/** The position of a recruiter in the queue of the non professional domains: the SHA-1 of the e-mail. */
	nonProfessionalQueuePosition,
	/** Marks the recruiter of the sign-up who is about to receive their first e-mail. */
	firstEmail,
	/** A document of the legacy index of candidates. */
	legacyCandidate,
	/** The link of the invitation to the form of the resume. */
	resumeFormLink,
	/** How many recruiters received the e-mail. */
	emailsSent,
	/** How many recruiters the run inactivated for not viewing a resume for too long. */
	recruitersInactivated,
	/** How many resumes were added or changed in the last days. */
	resumesFound,
	/** The run was skipped: the e-mails go out only from Monday to Friday. */
	weekend,
	/** The secret of the signature of the links of the e-mail, in the system properties. */
	recruiterLinksSecret,
}
