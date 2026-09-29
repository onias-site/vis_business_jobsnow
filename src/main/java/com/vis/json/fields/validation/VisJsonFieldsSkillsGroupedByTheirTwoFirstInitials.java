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

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	date,

	@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsPositionStatis.class)
	@CcpJsonFieldValidatorArray
	positionStatis,
	
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	skill, 

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	timestamp,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	word, 
	
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	parent,


}
