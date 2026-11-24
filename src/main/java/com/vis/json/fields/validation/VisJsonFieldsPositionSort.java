package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

/**
 * Validation rules of each item of the {@code sort} of the position: the preference of order of the resumes found for
 * the position.
 */
public enum VisJsonFieldsPositionSort implements CcpJsonFieldName {

	/** The {@code type} field: required, one of {@code VisPositionSortTypes}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionSortTypes.class)
	type,

	/** The {@code field} field: list, each one of {@code VisPositionSortFieldTypes}. */
	@CcpJsonFieldValidatorArray
	@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionSortFieldTypes.class)
	field,

	/**
	 * The {@code value} field: the name of the language of the criterion {@code spokenLanguage} or of the skill of the
	 * criterion {@code skill} (since 2026-11-23); absent in the other criteria.
	 */
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
	value,
	;
}
