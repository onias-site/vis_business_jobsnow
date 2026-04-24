package com.vis.entities;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDecoratorOperationType.delete;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDecoratorOperationType.save;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityOperationStepType.after;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityType.*;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityType.twinEntity;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityOperation;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityOperations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
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
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.decorators.JnAsyncWriterEntity;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.position.VisBusinessDuplicateFieldEmailToFieldMasters;
import com.vis.business.position.VisBusinessGroupPositionsGroupedByRecruiters;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;
import com.vis.utils.VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes;
@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityTwin(
		twinEntityName = "inactive_position",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityPosition.Fields.class)
@CcpEntityOperations(
		operations = {
				@CcpEntityOperation(when = after, operation = save, into = mainEntity,  execute = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}, operationHandlers = {}),
				//TODO VAI SAIR ESSE FLUXO POR CAUSA DA RETIRADA DOS GROUPINGS
				@CcpEntityOperation(when = after, operation = delete, into = mainEntity,  execute = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}, operationHandlers = {}),
				//TODO REVISITAR ESTE FLUXO
				@CcpEntityOperation(when = after, operation = delete, into = twinEntity,  execute = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}, operationHandlers = {}),
		},
		globalHandlers = {}
		)

public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	//DOUBT FUNCIONA ESTA VALIDAÇÃO?
	@CcpJsonGlobalValidations(requiresAtLeastOne = {
			@CcpJsonValidationFieldList({"maxClt", "maxPj" }),
			@CcpJsonValidationFieldList({"minClt", "minPj" })
	}, requiresAllOrNone = {
			@CcpJsonValidationFieldList({"maxClt", "minClt" }),
			@CcpJsonValidationFieldList({"minPj", "maxPj" })
	})
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
		@CcpJsonFieldTypeBoolean
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
