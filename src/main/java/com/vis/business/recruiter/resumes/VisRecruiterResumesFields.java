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
	/** The LinkedIn of the person who signs the e-mail. */
	oniasLinkedin,
	/** The skills and their positions in the ranking of {@code vis_skill}. */
	rankings,
	/** The number of resumes of the e-mail. */
	totalOfResumes,
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
	/** How many recruiters received the e-mail. */
	emailsSent,
	/** How many resumes were added or changed in the last days. */
	resumesFound,
	/** The run was skipped: the e-mails go out only from Monday to Friday. */
	weekend,
	/** The secret of the signature of the links of the e-mail, in the system properties. */
	recruiterLinksSecret,
}
