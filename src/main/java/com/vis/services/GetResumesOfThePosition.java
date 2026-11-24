package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityPositionResumes;

/** The input of {@code VisServiceRecruiterResumes.GetResumesOfThePosition}: the recruiter logged in and the title of the position. */
enum GetResumesOfThePosition implements CcpJsonFieldName{
	/** The recruiter logged in, the one that receives the resumes of the position. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The title of the position. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPositionResumes.Fields.class)
	@CcpJsonFieldValidatorRequired
	title,
}
