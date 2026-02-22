package com.vis.json.fields.validation;

import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

public enum VisJsonFieldsSkills {

	@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
	associated,
	
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	@CcpJsonFieldValidatorRequired
	category,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	parent,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	skill, 

	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	@CcpJsonFieldValidatorRequired
	type,
	

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	word, 
	
}
