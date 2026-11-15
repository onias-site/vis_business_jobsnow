package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.vis.business.recruiter.resumes.VisBusinessMoveViewedResume;

/** The input of {@code VisServiceRecruiterResumes.MarkViewedResumeAsUnwanted} ("Negativar CV"): the recruiter logged in, the month and the hash of the LinkedIn. */
enum MarkViewedResumeAsUnwanted implements CcpJsonFieldName{
	/** The recruiter logged in. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessMoveViewedResume.Fields.class)
	email,

	/** The month of the view ({@code yyyyMM}). */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessMoveViewedResume.Fields.class)
	month,

	/** The hash of the LinkedIn of the viewed resume. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessMoveViewedResume.Fields.class)
	linkedinHash,
}
