//package com.vis.json.validations;
//
//import com.ccp.decorators.CcpEmailDecorator;
//import com.ccp.validation.annotations.CcpJsonFieldsValidation;
//import com.ccp.validation.annotations.CcpObjectTextSize;
//import com.ccp.validation.annotations.CcpRegex;
//import com.ccp.validation.annotations.CcpSimpleObject;
//import com.ccp.validation.enums.CcpObjectTextSizeValidations;
//import com.ccp.validation.enums.CcpSimpleObjectValidations;
//
//@CcpJsonFieldsValidation(
//
//		regex = {
//				@CcpRegex(value = CcpEmailDecorator.EMAIL_REGEX, fields = "email")
//		},
//		simpleObject = { @CcpSimpleObject(rule = CcpSimpleObjectValidations.requiredFields, fields = { "resumeBase64",
//				"resumeText", }), }, objectTextSize = {
//						@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, fields = {
//								"resumeBase64" }, bound = 4_200_000),
//						@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, fields = {
//								"resumeText" }, bound = 2_100_000),
//						@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, fields = {
//								"resumeBase64" }, bound = 512),
//						@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, fields = {
//								"resumeText" }, bound = 512),
//		}
//
//)
//public class VisJsonFieldValidationResumeViewAdd {
//
//}
