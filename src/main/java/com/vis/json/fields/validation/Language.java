package com.vis.json.fields.validation;

import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

/** Validation rules of a language of a resume or position. */
enum Language {
	/** The name of the language: text of 3 to 20 characters. */
	@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
	name,

	/** The level: 1 or 2. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
	level 
}
