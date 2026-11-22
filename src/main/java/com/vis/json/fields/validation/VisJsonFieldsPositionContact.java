package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;

/**
 * Validation rules of each {@code contact} of the position: where the candidates reach the recruiter. The
 * {@code value} is validated according to the {@code type} ({@code VisJsonValidatorPositionContactValue}).
 */
@CcpJsonGlobalValidations(customJsonValidators = {VisJsonValidatorPositionContactValue.class})
public enum VisJsonFieldsPositionContact implements CcpJsonFieldName {

	/** The {@code type} field: required (the validation of the value depends on it), one of {@code VisPositionContactTypes}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionContactTypes.class)
	type,

	/** The {@code value} field: required, text of up to 500 characters, validated according to the {@code type}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 500)
	value,
	;
}
