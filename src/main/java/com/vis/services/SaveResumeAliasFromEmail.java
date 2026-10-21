package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityViewedResume;

/** The input of {@code VisServiceRecruiterResumes.SaveResumeAliasFromEmail}: the signed link and the alias. */
enum SaveResumeAliasFromEmail implements CcpJsonFieldName{
	/** The recruiter. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The id of the resume in {@code vis_resume}. */
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
	@CcpJsonFieldValidatorRequired
	resumeId,

	/** The signature of the link. */
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
	@CcpJsonFieldValidatorRequired
	token,

	/** The alias the recruiter gives to the resume. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityViewedResume.Fields.class)
	@CcpJsonFieldValidatorRequired
	alias,
}
