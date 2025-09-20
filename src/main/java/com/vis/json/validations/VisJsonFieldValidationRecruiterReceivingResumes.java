//package com.vis.json.validations;
//
//import com.ccp.decorators.CcpEmailDecorator;
//import com.ccp.validation.annotations.CcpAllowedValues;
//import com.ccp.validation.annotations.CcpDay;
//import com.ccp.validation.annotations.CcpJsonFieldsValidation;
//import com.ccp.validation.annotations.CcpObjectNumbers;
//import com.ccp.validation.annotations.CcpObjectTextSize;
//import com.ccp.validation.annotations.CcpRegex;
//import com.ccp.validation.annotations.CcpSimpleArray;
//import com.ccp.validation.annotations.CcpSimpleObject;
//import com.ccp.validation.enums.CcpAllowedValuesValidations;
//import com.ccp.validation.enums.CcpDayValidations;
//import com.ccp.validation.enums.CcpObjectNumberValidations;
//import com.ccp.validation.enums.CcpObjectTextSizeValidations;
//import com.ccp.validation.enums.CcpSimpleArrayValidations;
//import com.ccp.validation.enums.CcpSimpleObjectValidations;
//
//@CcpJsonFieldsValidation(
//		regex = {
//				@CcpRegex(value = CcpEmailDecorator.EMAIL_REGEX, fields = "email")
//		},
//		simpleObject = {
//				@CcpSimpleObject(rule = CcpSimpleObjectValidations.requiredFields, fields = { "title", "description",
//						"contactChannel", "expireDate", "mandatorySkill", "desiredSkill", "ddd", "seniority",
//						"disponibility", "pcd", "sortFields", "frequency", "channel", "showSalaryExpectation" }),
//				@CcpSimpleObject(rule = CcpSimpleObjectValidations.requiredAtLeastOne, fields = { "maxClt", "maxPj" }),
//				@CcpSimpleObject(rule = CcpSimpleObjectValidations.requiredAtLeastOne, fields = { "minClt", "minPj" }),
//				@CcpSimpleObject(rule = CcpSimpleObjectValidations.nonRepeatedLists, fields = { "ddd", "channel" , 
//						"desiredSkill", "mandatorySkill", "sortFields"}),
//				@CcpSimpleObject(rule = CcpSimpleObjectValidations.integerFields, fields = { "disponibility" }),
//				@CcpSimpleObject(rule = CcpSimpleObjectValidations.booleanFields, fields = { "pcd", "showSalaryExpectation" }),
//		},
//		allowedValues = { @CcpAllowedValues(rule = CcpAllowedValuesValidations.arrayWithAllowedNumbers, fields = {
//				"ddd" }, allowedValues = { "10", "61", "62", "64", "65", "66", "67", "82", "71", "73", "74", "75",
//						"77", "85", "88", "98", "99", "83", "81", "87", "86", "89", "84", "79", "68", "96", "92", "97",
//						"91", "93", "94", "69", "95", "63", "27", "28", "31", "32", "33", "34", "35", "37", "38", "21",
//						"22", "24", "11", "12", "13", "14", "15", "16", "17", "18", "19", "41", "42", "43", "44", "45",
//						"46", "51", "53", "54	", "55", "47", "48", "49" }),
//				@CcpAllowedValues(rule = CcpAllowedValuesValidations.objectWithAllowedTexts, fields = {
//						"seniority" }, allowedValues = { "JR", "PL", "SR", "ES" }),
//				@CcpAllowedValues(rule = CcpAllowedValuesValidations.arrayWithAllowedTexts, fields = {
//						"sortFields" }, allowedValues = { "seniority", "pj", "clt", "btc", "disponibility",
//								"desiredSkills"}),
//				@CcpAllowedValues(rule = CcpAllowedValuesValidations.objectWithAllowedTexts, fields = {
//						"frequency" }, allowedValues = { "minute", "hourly", "daily", "weekly", "monthly" }),
//				@CcpAllowedValues(rule = CcpAllowedValuesValidations.arrayWithAllowedTexts, fields = {
//						"channel" }, allowedValues = { "telegram", "whatsapp", "email", "sms" }),
//		},
//		simpleArray = {
//
//				@CcpSimpleArray(rule = CcpSimpleArrayValidations.notEmptyArray, fields = { "ddd", "mandatorySkill", "sortFields" }),
//				@CcpSimpleArray(rule = CcpSimpleArrayValidations.jsonItems, fields = { "desiredSkill" }),
//				@CcpSimpleArray(rule = CcpSimpleArrayValidations.integerItems, fields = { "ddd" }),
//		},
//		objectNumbers = {
//				@CcpObjectNumbers(rule = CcpObjectNumberValidations.equalsOrGreaterThan, bound = 0, fields = {
//						"disponibility" }),
//				@CcpObjectNumbers(rule = CcpObjectNumberValidations.equalsOrLessThan, bound = 365, fields = {
//						"disponibility" }),
//				@CcpObjectNumbers(rule = CcpObjectNumberValidations.equalsOrGreaterThan, bound = 1000, fields = { "minBtc",
//						"minClt", "minPj" }),
//				@CcpObjectNumbers(rule = CcpObjectNumberValidations.equalsOrLessThan, bound = 100000, fields = { "maxBtc", "maxClt",
//						"maxPj" }) },
//		objectTextSize = {
//				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, fields = { "title" }, bound = 3),
//				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, fields = { "title" }, bound = 100),
//				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, fields = {
//						"description" }, bound = 200),
//				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, fields = {
//						"description" }, bound = 10000),
//				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, fields = {
//						"contactChannel" }, bound = 3),
//				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, fields = {
//						"contactChannel" }, bound = 100),
//		},
//		day = { @CcpDay(rule = CcpDayValidations.equalsOrGreaterThan, fields = { "expireDate" }, bound = 0),
//				@CcpDay(rule = CcpDayValidations.equalsOrLessThan, fields = { "expireDate" }, bound = 365), }
//
//)
//public class VisJsonFieldValidationRecruiterReceivingResumes {
//
//}
