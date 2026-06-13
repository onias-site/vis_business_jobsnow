package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Define as regras de validação dos campos de cada objeto de skill dentro do índice
 * VisEntityGroupPositionsBySkills (agrupamento por duas primeiras letras). Inclui o campo positionStatis
 * para armazenar estatísticas de vagas associadas a cada skill.
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
