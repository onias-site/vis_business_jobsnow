package com.vis.json.fields.validation;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTime;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;

public enum VisJsonFieldsValidationCatalog{
	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeArray(minSize = 1, maxSize = 67)
	@CcpJsonFieldTypeNumber(allowedValues = { 10, 61, 62, 64, 65, 66, 67, 82,
			71, 73, 74, 75,77, 85, 88, 98, 99, 83, 81, 87, 86, 89, 84, 79, 68, 96, 
			92, 97,	91, 93, 94, 69, 95, 63, 27, 28, 31, 32, 33, 34, 35, 37, 38, 21,
			22, 24, 11, 12, 13, 14, 15, 16, 17, 18, 19, 41, 42, 43, 44, 45,
			46, 51, 53, 54	, 55, 47, 48, 49 })
	ddd, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(maxLength = 50, minLength = 2)
	domain, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 0, integerNumber = true)
	listSize, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	from, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	detail,
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	title, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(allowedValues = {"JR", "PL", "SR", "ES"})
	seniority, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.TimeAfterCurrentDate)
	@CcpJsonFieldTypeTime(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.monthly)
	disponibility, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.Boolean)
	pcd, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(regexValidation = CcpEmailDecorator.EMAIL_REGEX, minLength = 7, maxLength = 100)
	recruiter, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 0)
	fee, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	service,
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	@CcpJsonFieldTypeArray
	prerequisite, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 1, integerNumber = true)
	ranking,
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	@CcpJsonFieldTypeArray
	synonym, 
	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	email, 

	@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	@CcpJsonFieldTypeArray
	skill, 

}