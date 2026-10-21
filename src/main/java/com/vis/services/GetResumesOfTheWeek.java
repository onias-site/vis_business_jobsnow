package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/** The input of {@code VisServiceRecruiterResumes.GetResumesOfTheWeek}: the recruiter logged in. */
enum GetResumesOfTheWeek implements CcpJsonFieldName{
	/** The recruiter logged in. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,
}
