package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/** The input of {@code VisServiceRecruiterResumes.UnsubscribeFromResumesEmail}: the values of the signed link. */
enum UnsubscribeFromResumesEmail implements CcpJsonFieldName{
	/** The recruiter. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The signature of the link. */
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
	@CcpJsonFieldValidatorRequired
	token,
}
