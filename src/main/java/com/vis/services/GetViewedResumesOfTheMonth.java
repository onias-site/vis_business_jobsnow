package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.vis.business.recruiter.resumes.VisBusinessViewedResumesOfTheMonth;

/** The input of {@code VisServiceRecruiterResumes.GetViewedResumesOfTheMonth}: the recruiter logged in and the month. */
enum GetViewedResumesOfTheMonth implements CcpJsonFieldName{
	/** The recruiter logged in. */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
	email,

	/** The month of the views ({@code yyyyMM}). */
	@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
	month,
}
