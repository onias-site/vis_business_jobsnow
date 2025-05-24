package com.vis.commons.json.validations;

import com.ccp.constantes.CcpStringConstants;
import com.ccp.validation.annotations.ObjectTextSize;
import com.ccp.validation.annotations.Regex;
import com.ccp.validation.annotations.SimpleObject;
import com.ccp.validation.annotations.CcpJsonFieldsValidation;
import com.ccp.validation.enums.ObjectTextSizeValidations;
import com.ccp.validation.enums.SimpleObjectValidations;

@CcpJsonFieldsValidation(

		regex = {
				@Regex(value = CcpStringConstants.EMAIL_REGEX, fields = "email")
		},
		simpleObject = { @SimpleObject(rule = SimpleObjectValidations.requiredFields, fields = { "resumeBase64",
				"resumeText", }), }, objectTextSize = {
						@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, fields = {
								"resumeBase64" }, bound = 4_200_000),
						@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, fields = {
								"resumeText" }, bound = 2_100_000),
						@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, fields = {
								"resumeBase64" }, bound = 512),
						@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, fields = {
								"resumeText" }, bound = 512),
		}

)
public class VisJsonValidationSkillsSuggest {

}
