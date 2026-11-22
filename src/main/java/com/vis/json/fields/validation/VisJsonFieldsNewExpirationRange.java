package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeAfter;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;

/**
 * Validation rules of the {@code expiration} of a new position (screen "Meu Recrutamento - Vagas", button "Criar vaga"):
 * two timestamps (milliseconds), neither before the current date, the max not smaller than the min
 * ({@code VisJsonValidatorRangeMinNotAboveMax}). The stored position keeps only the order of the two
 * ({@code VisJsonFieldsExpirationRange}).
 */
@CcpJsonGlobalValidations(customJsonValidators = {VisJsonValidatorRangeMinNotAboveMax.class})
public enum VisJsonFieldsNewExpirationRange implements CcpJsonFieldName {

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
