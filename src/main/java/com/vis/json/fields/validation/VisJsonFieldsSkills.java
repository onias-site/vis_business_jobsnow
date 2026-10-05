package com.vis.json.fields.validation;

import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

/**
 * Defines the validation rules of the fields of a skill object inside a resume (nested JSON).
 * Used by VisEntityResume.Fields.skill as the reference to validate each item of the candidate's
 * skill list.
 */
public enum VisJsonFieldsSkills {

	/** The {@code associated} field: text of 2 to 50 characters. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	associated,
	
	/** The {@code category} field: required, text of 2 to 20 characters. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
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

	/** The {@code type} field: required, text of 2 to 20 characters. */
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	@CcpJsonFieldValidatorRequired
	type,
	

	/** The {@code word} field: required, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	word, 
	
}
