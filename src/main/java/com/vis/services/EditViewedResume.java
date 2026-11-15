package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.vis.business.recruiter.resumes.VisBusinessEditViewedResume;

/** The input of {@code VisServiceRecruiterResumes.EditViewedResume}: the recruiter logged in, the month, the hash of the LinkedIn, the alias and the comment. */
enum EditViewedResume implements CcpJsonFieldName{
	/** The recruiter logged in. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessEditViewedResume.Fields.class)
	email,

	/** The month of the view ({@code yyyyMM}). */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessEditViewedResume.Fields.class)
	month,

	/** The hash of the LinkedIn of the viewed resume. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessEditViewedResume.Fields.class)
	linkedinHash,

	/** The alias the recruiter gives to the resume. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessEditViewedResume.Fields.class)
	alias,

	/** The comment of the recruiter about the resume. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessEditViewedResume.Fields.class)
	comment,
}
