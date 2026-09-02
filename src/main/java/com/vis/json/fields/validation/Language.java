package com.vis.json.fields.validation;

import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

enum Language {
	@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
	name,

	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
	level
}
