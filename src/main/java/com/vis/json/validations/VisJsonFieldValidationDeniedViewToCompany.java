package com.vis.json.validations;

import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;

public class VisJsonFieldValidationDeniedViewToCompany {
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(maxLength = 50, minLength = 2)
	Object domain;

}
