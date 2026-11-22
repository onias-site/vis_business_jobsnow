package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * The input of {@code VisServiceRecruiterResumes.GetResumesOfThePositionFilters}: the recruiter logged in; the ids of the
 * resumes are validated by {@code VisBusinessResumesOfThePositionFilters}.
 */
enum GetResumesOfThePositionFilters implements CcpJsonFieldName{
	/** The recruiter logged in. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,
}
