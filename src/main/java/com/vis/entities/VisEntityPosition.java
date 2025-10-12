package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntityDecorators;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntityFieldTransformer;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeBefore;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.position.VisBusinessDuplicateFieldEmailToFieldMasters;
import com.vis.business.position.VisBusinessExtractSkillsFromPositionText;
import com.vis.business.position.VisBusinessGroupPositionsGroupedByRecruiters;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;
import com.vis.utils.VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(
		twinEntityName = "inactive_position"
		,afterRecordBeenTransportedFromTwinToMainEntity = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}
		,afterRecordBeenTransportedFromMainToTwinEntity = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}
		)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		afterSaveRecord = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class},
		beforeSaveRecord = {VisBusinessExtractSkillsFromPositionText.class},
		entityValidation = VisEntityPosition.Fields.class,
		cacheableEntity = true, 
		afterDeleteRecord = {}, 
		flow = {} 
)
@CcpJsonGlobalValidations(requiresAtLeastOne = {
		@CcpJsonValidationFieldList({"maxClt", "maxPj" }),
		@CcpJsonValidationFieldList({"minClt", "minPj" })
}, requiresAllOrNone = {
		@CcpJsonValidationFieldList({"maxClt", "minClt" }),
		@CcpJsonValidationFieldList({"minPj", "maxPj" })
})
public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValues = {  "telegram", "whatsapp", "email", "sms" })
		@CcpJsonFieldValidatorArray
		channel, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
		contactChannel, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10_000)
		@CcpEntityFieldTransformer(VisBusinessExtractSkillsFromText.class)
		description, 
		@CcpJsonFieldTypeNestedJson
		@CcpJsonFieldValidatorArray
		desiredSkill, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerPutEmailHashAndDomainRecruiter.class)
		email, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeTimeBefore(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.yearly)
		expireDate, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValues = { "minute", "hourly", "daily", "weekly", "monthly" })
		frequency, 
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		pcd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		requiredSkill, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		seniority, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValues = { "seniority", "pj", "clt", "btc", "disponibility", "desiredSkills" })
		@CcpJsonFieldValidatorArray(minSize = 1)
		sortFields, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		title, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeBoolean
		showSalaryExpectation,
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minBtc,
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxBtc,
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minClt,
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxClt,
		@CcpJsonFieldTypeNumber(minValue = 1_000)
		minPj,
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxPj,
		;
	}
}
