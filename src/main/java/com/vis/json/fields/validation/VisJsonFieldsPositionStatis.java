package com.vis.json.fields.validation;

import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.json.fields.validation.JnJsonCommonsFields;


enum SalaryType{
	pj, clt
}

@CcpJsonGlobalValidations(
		requiresAtLeastOne = {
		@CcpJsonValidationFieldList(SalaryType.class)
})
/**
 * Defines the validation rules of the position statistics fields, used as a nested JSON validation
 * schema. Requires at least one of the pj or clt fields to be present. Includes availability, DDD,
 * experience, seniority, PCD, language and salary range fields.
 */
public enum VisJsonFieldsPositionStatis {

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	btc,

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	clt,

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	date,
	
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	disponibility,

	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	ddd,
	
	@CcpEntityFieldPrimaryKey
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	email,

	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	experience,

	@CcpJsonFieldValidatorArray
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	language,

	@CcpJsonFieldTypeBoolean
	negotiableClaim,
	
	@CcpJsonFieldTypeBoolean
	pcd,

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	pj,

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	timestamp,

	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	temporallyJobTime,
	
	@CcpEntityFieldPrimaryKey
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	title, 

	@CcpJsonFieldTypeBoolean
	travel,


}
