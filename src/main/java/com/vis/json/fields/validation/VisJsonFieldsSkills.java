package com.vis.json.fields.validation;

import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

/**
 * Define as regras de validação dos campos de um objeto de skill dentro de um currículo (nested JSON).
 * Usado em VisEntityResume.Fields.skill como referência para validação de cada item da lista de skills
 * do candidato.
 */
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
