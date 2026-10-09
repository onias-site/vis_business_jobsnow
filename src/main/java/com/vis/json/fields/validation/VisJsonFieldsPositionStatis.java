package com.vis.json.fields.validation;

import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Validation rules of the position statistics, used as a nested JSON schema: at least one of {@code pj} or
 * {@code clt} is required.
 */
@CcpJsonGlobalValidations(
		requiresAtLeastOne = {
		@CcpJsonValidationFieldList(SalaryType.class)
})
public enum VisJsonFieldsPositionStatis {

	/** The {@code btc} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	btc,

	/** The {@code clt} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	clt,

	/** The {@code date} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	date,
	
	/** The {@code disponibility} field: required, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	disponibility,

	/** The {@code ddd} field: required, list of 1 to 67 area codes, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	ddd,
	
	/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
	@CcpEntityFieldPrimaryKey
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	email,

	/** The {@code experience} field: required, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	experience,

	/** The {@code language} field: list, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonFieldValidatorArray
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	language,

	/** The {@code negotiableClaim} field: boolean. */
	@CcpJsonFieldTypeBoolean
	negotiableClaim,
	
	/** The {@code pcd} field: boolean, whether the position is for people with a disability. */
	@CcpJsonFieldTypeBoolean
	pcd,

	/** The {@code pj} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	pj,

	/** The {@code timestamp} field: validated as in {@code JnJsonCommonsFields}. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	timestamp,

	/** The {@code temporallyJobTime} field: required, validated as in {@code VisJsonCommonsFields}. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	temporallyJobTime,
	
	/** The {@code title} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
	@CcpEntityFieldPrimaryKey
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	title, 

	/** The {@code travel} field: boolean, whether the position requires travel. */
	@CcpJsonFieldTypeBoolean
	travel,


}
