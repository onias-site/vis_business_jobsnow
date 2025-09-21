package com.vis.json.validations;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNested;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTime;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;
import com.ccp.json.validations.global.annotations.CcpJsonValidatorGlobal;
import com.ccp.json.validations.global.annotations.CcpJsonValidatorFieldList;

@CcpJsonValidatorGlobal(requiresAtLeastOne = {
		@CcpJsonValidatorFieldList({"maxClt", "maxPj" }),
		@CcpJsonValidatorFieldList({"minClt", "minPj" })
}, requiresAllOrNone = {
		@CcpJsonValidatorFieldList({"maxClt", "minClt" }),
		@CcpJsonValidatorFieldList({"minPj", "maxPj" })
		
})
public class VisJsonFieldValidationPosition {
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(regexValidation = CcpEmailDecorator.EMAIL_REGEX, minLength = 7, maxLength = 100)
	Object email;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
	Object title;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 10, maxLength = 10000)
	Object description;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
	Object contactChannel;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Time)
	@CcpJsonFieldTypeTime(minValue = 0, maxValue = 30, intervalType = CcpEntityExpurgableOptions.daily)
	Object expireDate;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeArray(nonRepeatedItems = true, minSize = 1)
	Object mandatorySkill;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Json)
	@CcpJsonFieldTypeNested
	@CcpJsonFieldTypeArray
	Object desiredSkill;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeArray(nonRepeatedItems = true, minSize = 1)
	@CcpJsonFieldTypeNumber(allowedValues = { 10, 61, 62, 64, 65, 66, 67, 82, 71, 73, 74, 75,
			77, 85, 88, 98, 99, 83, 81, 87, 86, 89, 84, 79, 68, 96, 92, 97,
			91, 93, 94, 69, 95, 63, 27, 28, 31, 32, 33, 34, 35, 37, 38, 21,
			22, 24, 11, 12, 13, 14, 15, 16, 17, 18, 19, 41, 42, 43, 44, 45,
			46, 51, 53, 54	, 55, 47, 48, 49 })
	Object ddd;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(allowedValues = { "JR", "PL", "SR", "ES" })
	@CcpJsonFieldTypeArray(nonRepeatedItems = true)
	Object seniority;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeTime(minValue = 0, maxValue = 365, intervalType = CcpEntityExpurgableOptions.daily)
	Object disponibility;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Boolean)
	Object pcd;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(allowedValues = { "seniority", "pj", "clt", "btc", "disponibility", "desiredSkills" })
	@CcpJsonFieldTypeArray(nonRepeatedItems = true, minSize = 1)
	Object sortFields;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(allowedValues = { "minute", "hourly", "daily", "weekly", "monthly" })
	Object frequency;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(allowedValues = {  "telegram", "whatsapp", "email", "sms" })
	@CcpJsonFieldTypeArray(nonRepeatedItems = true)
	Object channel;

	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Boolean)
	Object showSalaryExpectation;
	
	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 1000)
	Object minBtc;

	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(maxValue = 100000)
	Object maxBtc;

	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 1000)
	Object minClt;

	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(maxValue = 100000)
	Object maxClt;

	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 1000)
	Object minPj;

	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(maxValue = 100000)
	Object maxPj;

}

