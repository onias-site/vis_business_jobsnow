package com.vis.json.fields.validation;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

public enum VisJsonCommonsFields{
	
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = { 10, 61, 62, 64, 65, 66, 67, 82,
			71, 73, 74, 75,77, 85, 88, 98, 99, 83, 81, 87, 86, 89, 84, 79, 68, 96, 
			92, 97,	91, 93, 94, 69, 95, 63, 27, 28, 31, 32, 33, 34, 35, 37, 38, 21,
			22, 24, 11, 12, 13, 14, 15, 16, 17, 18, 19, 41, 42, 43, 44, 45,
			46, 51, 53, 54	, 55, 47, 48, 49 })
	ddd, 
	@CcpJsonFieldTypeString(maxLength = 50, minLength = 2)
	domain, 
	@CcpJsonFieldTypeNumberUnsigned
	listSize, 
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	from, 
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	detail,
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	title, 
	@CcpJsonFieldTypeString(allowedValues = {"JR", "PL", "SR", "ES"})
	seniority, 
	@CcpJsonFieldTypeNumberUnsigned(maxValue = 30)
	disponibility, 
	@CcpJsonFieldTypeNumberUnsigned(maxValue = 12)
	temporallyJobTime, 
	@CcpJsonFieldTypeString(regexValidation = CcpEmailDecorator.EMAIL_REGEX, minLength = 7, maxLength = 100)
	recruiter, 
	@CcpJsonFieldTypeNumber(minValue = 0)
	fee, 
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	service,
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	prerequisite, //DOUBT OU SERIA "PARENT"?
	@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
	ranking,
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	synonym, 
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	email, 
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	skill, 
	
	@CcpJsonFieldTypeNestedJson(jsonValidation = Language.class)
	language
	;
	enum Language{
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		name,

		@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2} )
		level
	}

}