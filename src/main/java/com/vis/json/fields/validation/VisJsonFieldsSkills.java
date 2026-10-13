package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;

/**
 * Defines the validation rules of the fields of a skill object inside a resume (nested JSON).
 * Used by VisEntityResume.Fields.skill as the reference to validate each item of the candidate's
 * skill list.
 * The {@code type} must be one of the types its {@code category} accepts ({@code VisJsonValidatorSkillTypeMatchesCategory}).
 */
@CcpJsonGlobalValidations(customJsonValidators = {VisJsonValidatorSkillTypeMatchesCategory.class})
public enum VisJsonFieldsSkills implements CcpJsonFieldName{

	/** The {@code associated} field: text of 2 to 50 characters. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	associated,
	
	/** The {@code category} field: required, one of {@code VisResumeSkillCategories}. */
	@CcpJsonFieldTypeString(allowedValuesEnum = VisResumeSkillCategories.class)
	@CcpJsonFieldValidatorRequired
	category,

	/** The {@code parent} field: list, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	parent,

	/** The {@code skill} field: required, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	skill, 

	/** The {@code type} field: required, one of {@code VisResumeSkillTypes}. */
	@CcpJsonFieldTypeString(allowedValuesEnum = VisResumeSkillTypes.class)
	@CcpJsonFieldValidatorRequired
	type,
	

	/** The {@code word} field: required, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	word, 
	
}
