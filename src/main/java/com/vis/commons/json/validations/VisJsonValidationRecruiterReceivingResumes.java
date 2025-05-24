package com.vis.commons.json.validations;

import com.ccp.constantes.CcpStringConstants;
import com.ccp.validation.annotations.AllowedValues;
import com.ccp.validation.annotations.CcpJsonFieldsValidation;
import com.ccp.validation.annotations.Day;
import com.ccp.validation.annotations.ObjectNumbers;
import com.ccp.validation.annotations.ObjectTextSize;
import com.ccp.validation.annotations.Regex;
import com.ccp.validation.annotations.SimpleArray;
import com.ccp.validation.annotations.SimpleObject;
import com.ccp.validation.enums.AllowedValuesValidations;
import com.ccp.validation.enums.DayValidations;
import com.ccp.validation.enums.ObjectNumberValidations;
import com.ccp.validation.enums.ObjectTextSizeValidations;
import com.ccp.validation.enums.SimpleArrayValidations;
import com.ccp.validation.enums.SimpleObjectValidations;

@CcpJsonFieldsValidation(
		regex = {
				@Regex(value = CcpStringConstants.EMAIL_REGEX, fields = "email")
		},
		simpleObject = {
				@SimpleObject(rule = SimpleObjectValidations.requiredFields, fields = { "title", "description",
						"contactChannel", "expireDate", "mandatorySkill", "desiredSkill", "ddd", "seniority",
						"disponibility", "pcd", "sortFields", "frequency", "channel", "showSalaryExpectation" }),
				@SimpleObject(rule = SimpleObjectValidations.requiredAtLeastOne, fields = { "maxClt", "maxPj" }),
				@SimpleObject(rule = SimpleObjectValidations.requiredAtLeastOne, fields = { "minClt", "minPj" }),
				@SimpleObject(rule = SimpleObjectValidations.nonRepeatedLists, fields = { "ddd", "channel" , 
						"desiredSkill", "mandatorySkill", "sortFields"}),
				@SimpleObject(rule = SimpleObjectValidations.integerFields, fields = { "disponibility" }),
				@SimpleObject(rule = SimpleObjectValidations.booleanFields, fields = { "pcd", "showSalaryExpectation" }),
		},
		allowedValues = { @AllowedValues(rule = AllowedValuesValidations.arrayWithAllowedNumbers, fields = {
				"ddd" }, allowedValues = { "10", "61", "62", "64", "65", "66", "67", "82", "71", "73", "74", "75",
						"77", "85", "88", "98", "99", "83", "81", "87", "86", "89", "84", "79", "68", "96", "92", "97",
						"91", "93", "94", "69", "95", "63", "27", "28", "31", "32", "33", "34", "35", "37", "38", "21",
						"22", "24", "11", "12", "13", "14", "15", "16", "17", "18", "19", "41", "42", "43", "44", "45",
						"46", "51", "53", "54	", "55", "47", "48", "49" }),
				@AllowedValues(rule = AllowedValuesValidations.objectWithAllowedTexts, fields = {
						"seniority" }, allowedValues = { "JR", "PL", "SR", "ES" }),
				@AllowedValues(rule = AllowedValuesValidations.arrayWithAllowedTexts, fields = {
						"sortFields" }, allowedValues = { "seniority", "pj", "clt", "btc", "disponibility",
								"desiredSkills"}),
				@AllowedValues(rule = AllowedValuesValidations.objectWithAllowedTexts, fields = {
						"frequency" }, allowedValues = { "minute", "hourly", "daily", "weekly", "monthly" }),
				@AllowedValues(rule = AllowedValuesValidations.arrayWithAllowedTexts, fields = {
						"channel" }, allowedValues = { "telegram", "whatsapp", "email", "sms" }),
		},
		simpleArray = {

				@SimpleArray(rule = SimpleArrayValidations.notEmptyArray, fields = { "ddd", "mandatorySkill", "sortFields" }),
				@SimpleArray(rule = SimpleArrayValidations.jsonItems, fields = { "desiredSkill" }),
				@SimpleArray(rule = SimpleArrayValidations.integerItems, fields = { "ddd" }),
		},
		objectNumbers = {
				@ObjectNumbers(rule = ObjectNumberValidations.equalsOrGreaterThan, bound = 0, fields = {
						"disponibility" }),
				@ObjectNumbers(rule = ObjectNumberValidations.equalsOrLessThan, bound = 365, fields = {
						"disponibility" }),
				@ObjectNumbers(rule = ObjectNumberValidations.equalsOrGreaterThan, bound = 1000, fields = { "minBtc",
						"minClt", "minPj" }),
				@ObjectNumbers(rule = ObjectNumberValidations.equalsOrLessThan, bound = 100000, fields = { "maxBtc", "maxClt",
						"maxPj" }) },
		objectTextSize = {
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, fields = { "title" }, bound = 3),
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, fields = { "title" }, bound = 100),
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, fields = {
						"description" }, bound = 200),
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, fields = {
						"description" }, bound = 10000),
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, fields = {
						"contactChannel" }, bound = 3),
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, fields = {
						"contactChannel" }, bound = 100),
		},
		day = { @Day(rule = DayValidations.equalsOrGreaterThan, fields = { "expireDate" }, bound = 0),
				@Day(rule = DayValidations.equalsOrLessThan, fields = { "expireDate" }, bound = 365), }

)
public class VisJsonValidationRecruiterReceivingResumes {

}
