package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.fields.validation.JnJsonCommonsFields;

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
	
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 35)
	@CcpJsonFieldValidatorArray
	parent,


}
