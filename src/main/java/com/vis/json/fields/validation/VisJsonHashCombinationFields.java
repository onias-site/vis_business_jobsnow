package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.vis.json.transformers.VisSeniorityType;

/**
 * Validation rules of the fields of {@code VisEntityVirtualHashGrouper} whose values differ from the same fields of the
 * resume: the combinations of {@code VisResumeHashCombinations} turn the flags into 1 (the search does not require it)
 * or 2 (the search requires it), the seniority into the constants of {@code VisSeniorityType}, and add the neutral
 * values 0 to the ddd (any region) and to the language level (no language required). A field of the grouper takes these
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

	/** The {@code ddd} field: the ddds accepted by {@code VisJsonCommonsFields.ddd}, plus 0 (any region). */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {
			0,
			10, 61, 62, 64, 65, 66, 67, 82, 71, 73, 74, 75, 77, 85, 88, 98, 99,
			83, 81, 87, 86, 89, 84, 79, 68, 96, 92, 97, 91, 93, 94, 69, 95, 63,
			27, 28, 31, 32, 33, 34, 35, 37, 38, 21, 22, 24, 11, 12, 13, 14, 15,
			16, 17, 18, 19, 41, 42, 43, 44, 45, 46, 51, 53, 54, 55, 47, 48, 49
	})
	ddd,
	;
}
