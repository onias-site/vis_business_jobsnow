package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;

/**
 * Validation rules of a salary range of the position ({@code clt} and {@code pj} of {@code VisEntityPosition}): both
 * ends between 1,000 and 100,000, the max not smaller than the min ({@code VisJsonValidatorRangeMinNotAboveMax}).
 */
@CcpJsonGlobalValidations(customJsonValidators = {VisJsonValidatorRangeMinNotAboveMax.class})
public enum VisJsonFieldsSalaryRange implements CcpJsonFieldName {

	/** The {@code min} field: required, decimal number from 1,000 to 100,000. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeNumber(minValue = 1_000, maxValue = 100_000)
	min,

	/** The {@code max} field: required, decimal number from 1,000 to 100,000. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeNumber(minValue = 1_000, maxValue = 100_000)
	max,
	;
}
