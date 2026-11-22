package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeAfter;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;

/**
 * Validation rules of the {@code expiration} of the position: two timestamps (milliseconds), neither before the current
 * date, the max not smaller than the min ({@code VisJsonValidatorRangeMinNotAboveMax}).
 */
@CcpJsonGlobalValidations(customJsonValidators = {VisJsonValidatorRangeMinNotAboveMax.class})
public enum VisJsonFieldsExpirationRange implements CcpJsonFieldName {

	/** The {@code min} field: required, timestamp from the current date on. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeTimeAfter(minValue = 0, intervalType = CcpEntityExpurgableOptions.daily)
	min,

	/** The {@code max} field: required, timestamp from the current date on, not before the min. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeTimeAfter(minValue = 0, intervalType = CcpEntityExpurgableOptions.daily)
	max,
	;
}
