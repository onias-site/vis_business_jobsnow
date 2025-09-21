package com.vis.json.validations;

import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;

public class VisJsonFieldValidationBalance {
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 0)
	Object balance;
	
}
