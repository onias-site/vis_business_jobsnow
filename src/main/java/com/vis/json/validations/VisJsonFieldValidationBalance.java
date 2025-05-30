package com.vis.json.validations;

import com.ccp.validation.annotations.CcpSimpleObject;
import com.ccp.validation.annotations.CcpJsonFieldsValidation;
import com.ccp.validation.enums.CcpSimpleObjectValidations;

@CcpJsonFieldsValidation(simpleObject = {
		@CcpSimpleObject(fields = "balance", rule = CcpSimpleObjectValidations.requiredFields)
	}
)
public class VisJsonFieldValidationBalance {

}
