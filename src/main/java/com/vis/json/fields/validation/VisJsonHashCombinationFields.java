package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.vis.json.transformers.VisSeniorityType;

/**
 * Validation rules of the fields of {@code VisEntityVirtualHashGrouper} whose values differ from the same fields of the
 * resume: the combinations of {@code VisResumeHashCombinations} turn the flags into 1 (the search does not require it)
 * or 2 (the search requires it), the seniority into the constants of {@code VisSeniorityType}, and add the neutral
 * value 0 to the language level (no language required). A field of the grouper takes these
 * rules through {@code @CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)}.
 */
public enum VisJsonHashCombinationFields implements CcpJsonFieldName {

	/** The {@code pcd} field: 1 or 2. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
	pcd,

	/** The {@code travel} field: 1 or 2. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
	travel,

	/** The {@code negotiableClaim} field: 1 or 2. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
	negotiableClaim,

	/** The {@code seniority} field: one of {@code VisSeniorityType}. */
	@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityType.class)
	seniority,

	/** The {@code languageLevel} field: 0 (no language required), 1 or 2. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {0, 1, 2})
	languageLevel,
	;
}
