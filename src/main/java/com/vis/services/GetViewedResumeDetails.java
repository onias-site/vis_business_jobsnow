package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.vis.business.recruiter.resumes.VisBusinessViewedResumeDetails;

/** The input of {@code VisServiceRecruiterResumes.GetViewedResumeDetails}: the recruiter logged in, the month and the LinkedIn. */
enum GetViewedResumeDetails implements CcpJsonFieldName{
	/** The recruiter logged in. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumeDetails.Fields.class)
	email,

	/** The month of the view ({@code yyyyMM}). */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumeDetails.Fields.class)
	month,

	/** The LinkedIn of the viewed resume. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumeDetails.Fields.class)
	linkedin,
}
