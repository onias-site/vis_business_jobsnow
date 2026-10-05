package com.vis.json.fields.validation;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeBefore;


/**
 * Defines the validations shared by the most common JSON fields of the VIS module, serving as a source
 * of reusable rules via @CcpJsonCopyFieldValidationsFrom in other entities. Centralizes the type, size
 * and allowed-value constraints of recurring fields such as email, seniority, ddd, skill,
 * recruiter, among others.
 */
public enum VisJsonCommonsFields implements CcpJsonFieldName {

	/** The {@code btc} field: decimal number. */
	@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_000)
	btc,

	/** The {@code clt} field: decimal number. */
	@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_500)
	clt, 

	/** The {@code detail} field: text. */
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	detail,

	/** The {@code disponibility} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned(maxValue = 30)
	disponibility,

	/** The {@code domain} field: text. */
	@CcpJsonFieldTypeString(maxLength = 50, minLength = 2)
	domain,

	/** The {@code ddd} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {
			10, 61, 62, 64, 65, 66, 67, 82, 71, 73, 74, 75, 77, 85, 88, 98, 99,
			83, 81, 87, 86, 89, 84, 79, 68, 96, 92, 97, 91, 93, 94, 69, 95, 63,
			27, 28, 31, 32, 33, 34, 35, 37, 38, 21, 22, 24, 11, 12, 13, 14, 15,
			16, 17, 18, 19, 41, 42, 43, 44, 45, 46, 51, 53, 54, 55, 47, 48, 49
	})
	ddd,

	/** The {@code email} field: text. */
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	email,

	/** The {@code experience} field: past timestamp. */
	@CcpJsonFieldTypeTimeBefore(maxValue = 70, intervalType = CcpEntityExpurgableOptions.yearly)
	experience,

	/** The {@code fee} field: decimal number. */
	@CcpJsonFieldTypeNumber(minValue = 0)
	fee,

	/** The {@code from} field: text. */
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	from,

	/** The {@code language} field: nested JSON. */
	@CcpJsonFieldTypeNestedJson(jsonValidation = Language.class)
	language,

	/** The {@code listSize} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned
	listSize,

	/** The {@code pj} field: decimal number. */
	@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 2_500)
	pj,

	/** The {@code ranking} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
	ranking,

	/** The {@code recruiter} field: text. */
	@CcpJsonFieldTypeString(regexValidation = CcpEmailDecorator.EMAIL_REGEX, minLength = 7, maxLength = 100)
	recruiter,
	
	/** The {@code resumeType} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1,2,3,4})
	resumeType,
	
	/** The {@code service} field: text. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	service,

	/** The {@code seniority} field: text. */
	@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityTypes.class)
	seniority,

	/** The {@code skill} field: text. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	skill,

	/** The {@code synonym} field: text. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	synonym,

	/** The {@code temporallyJobTime} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned(maxValue = 12)
	temporallyJobTime,

	/** The {@code title} field: text. */
	@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
	title,
	
	/** The {@code word} field: text. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	word, 

	/** The {@code parent} field: text. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	parent,

	/*
	 * The fields below were centralized from local enums that declared them in duplicate.
	 * They deliberately have no validation annotation: the centralization only unifies the key
	 * name, preserving the previous behavior.
	 */
	/** The {@code masters} field. */
	masters,

	/** The {@code resumeId} field. */
	resumeId,

	/** The {@code resumes} field. */
	resumes,

	/** The {@code viewMode} field. */
	viewMode,

	/** The {@code id} field. */
	id,

	/** The {@code label} field. */
	label,

	/** The {@code resumesCount} field. */
	resumesCount,

	;


}
