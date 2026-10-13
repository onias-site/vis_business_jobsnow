package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Input rules of the services of the resume that take only its key ({@code VisServiceResume.GetData}, {@code Delete} and
 * {@code ChangeStatus}): the e-mail of the owner, which the session puts in the request.
 */
public enum VisJsonFieldsResumeKey implements CcpJsonFieldName {

	/** The {@code email} field: required, validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	email,
}
