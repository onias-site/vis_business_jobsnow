package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Defines the validation rules of the fields of each skill object inside the
 * VisEntityGroupPositionsBySkills index (grouping by the first two letters). Includes the positionStatis
 * field to store the statistics of the positions associated with each skill.
 */
public enum VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials implements CcpJsonFieldName{

	/** The {@code date} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	date,

	/** The {@code positionStatis} field: nested JSON, list. */
	@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsPositionStatis.class)
	@CcpJsonFieldValidatorArray
	positionStatis,
	
	/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	skill, 

	/** The {@code timestamp} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	timestamp,

	/** The {@code word} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	word, 
	
	/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, list. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	parent,


}
