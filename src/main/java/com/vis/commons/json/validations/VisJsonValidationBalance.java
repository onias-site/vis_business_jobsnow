package com.vis.commons.json.validations;

import com.ccp.validation.annotations.SimpleObject;
import com.ccp.validation.annotations.CcpJsonFieldsValidation;
import com.ccp.validation.enums.SimpleObjectValidations;

@CcpJsonFieldsValidation(simpleObject = {
		@SimpleObject(fields = "balance", rule = SimpleObjectValidations.requiredFields)
	}
)
public class VisJsonValidationBalance {

}
