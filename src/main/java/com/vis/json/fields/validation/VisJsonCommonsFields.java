package com.vis.json.fields.validation;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
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
	@CcpJsonFieldTypeNestedJson(jsonValidation = VisLanguages.class)
	language,

	/** The {@code spokenLanguage} field: nested JSON, with the same rules of {@code language}. */
	@CcpJsonFieldTypeNestedJson(jsonValidation = VisLanguages.class)
	spokenLanguage,

	/** The {@code hash} field: text, the id of one record of {@code VisEntityVirtualHashGrouper}. */
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
	hash,

	/** The {@code languageName} field: text of 3 to 20 characters, the same rule of the {@code name} of {@code VisLanguages}. */
	@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
	languageName,

	/** The {@code languageLevel} field: 1 or 2, the same rule of the {@code level} of {@code VisLanguages}. */
	@CcpJsonFieldTypeNumberUnsigned(allowedValues = {1, 2})
	languageLevel,

	/** The {@code listSize} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned
	listSize,

	/** The {@code negotiableClaim} field: boolean. */
	@CcpJsonFieldTypeBoolean
	negotiableClaim,

	/** The {@code pcd} field: boolean. */
	@CcpJsonFieldTypeBoolean
	pcd,

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

	/**
	 * The {@code skill} field: text with at least two characters that are not blank, which is the same as more than one
	 * character after a trim ({@code " X "} is refused).
	 */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50, regexValidation = "(?s).*\\S.*\\S.*")
	skill,

	/** The {@code skillName} field: text, with the same rules of {@code skill}. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50, regexValidation = "(?s).*\\S.*\\S.*")
	skillName,

	/** The {@code synonym} field: text, with the same rules of {@code skill}. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50, regexValidation = "(?s).*\\S.*\\S.*")
	synonym,

	/** The {@code temporallyJobTime} field: non-negative integer. */
	@CcpJsonFieldTypeNumberUnsigned(maxValue = 12)
	temporallyJobTime,

	/** The {@code travel} field: boolean. */
	@CcpJsonFieldTypeBoolean
	travel,

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
