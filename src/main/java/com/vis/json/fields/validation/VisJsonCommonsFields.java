package com.vis.json.fields.validation;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeBefore;

public enum VisJsonCommonsFields {

	@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_000)
	btc,

	@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_500)
	clt,

	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	detail,

	@CcpJsonFieldTypeNumberUnsigned(maxValue = 30)
	disponibility,

	@CcpJsonFieldTypeString(maxLength = 50, minLength = 2)
	domain,

	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {
			10, 61, 62, 64, 65, 66, 67, 82, 71, 73, 74, 75, 77, 85, 88, 98, 99,
			83, 81, 87, 86, 89, 84, 79, 68, 96, 92, 97, 91, 93, 94, 69, 95, 63,
			27, 28, 31, 32, 33, 34, 35, 37, 38, 21, 22, 24, 11, 12, 13, 14, 15,
			16, 17, 18, 19, 41, 42, 43, 44, 45, 46, 51, 53, 54, 55, 47, 48, 49
	})
	ddd,

	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	email,

	@CcpJsonFieldTypeTimeBefore(maxValue = 70, intervalType = CcpEntityExpurgableOptions.yearly)
	experience,

	@CcpJsonFieldTypeNumber(minValue = 0)
	fee,

	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	from,

	@CcpJsonFieldTypeNestedJson(jsonValidation = Language.class)
	language,

	@CcpJsonFieldTypeNumberUnsigned
	listSize,

	@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 2_500)
	pj,

	@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
	ranking,

	@CcpJsonFieldTypeString(regexValidation = CcpEmailDecorator.EMAIL_REGEX, minLength = 7, maxLength = 100)
	recruiter,
	
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1,2,3,4})
	resumeType,
	
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	service,

	@CcpJsonFieldTypeString(allowedValues = {"JR", "PL", "SR", "ES"})
	seniority,

	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	skill,

	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	synonym,

	@CcpJsonFieldTypeNumberUnsigned(maxValue = 12)
	temporallyJobTime,

	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	title,
	
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	word, 

	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	parent,


	;

	enum Language {
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		name,

		@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
		level
	}
}