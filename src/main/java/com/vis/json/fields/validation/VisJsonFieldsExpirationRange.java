package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;

/**
 * Validation rules of the {@code expiration} of the stored position: two timestamps (milliseconds), the max not smaller
 * than the min ({@code VisJsonValidatorRangeMinNotAboveMax}). The dates may be in the past: the inactivation puts the max
 * one minute before now and an old position keeps its min. A new position, and a reactivation, ask for dates from the
 * current date on ({@code VisJsonFieldsNewExpirationRange}).
 */
@CcpJsonGlobalValidations(customJsonValidators = {VisJsonValidatorRangeMinNotAboveMax.class})
public enum VisJsonFieldsExpirationRange implements CcpJsonFieldName {

	/** The {@code min} field: required, timestamp of the beginning of the position. */
	@CcpJsonFieldValidatorRequired
	min,

	/** The {@code max} field: required, timestamp of the expiration of the position, not before the min. */
	@CcpJsonFieldValidatorRequired
	max,
	;
}
