package com.vis.entities;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
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
import com.jn.entities.decorators.annotations.JnEntityAsyncWriter;
import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.builders.JnEntityAsyncWriterBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;

/**
 * Represents the core Position entity of the system. Stores all the data of a position published
 * by a recruiter: job title, seniority, location (DDD), availability, contact channels,
 * required and desired skills, salary range (CLT, PJ, BTC), resume sending frequency,
 * expiration date and sorting criteria. Uses the Twin Entity pattern to control inactive positions
 * (inactive_position), has asynchronous writing, versioning and a 1-hour cache. On save or delete,
 * it triggers the regrouping flows and the sending of resumes to recruiters.
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8),})
@CcpEntityTwin(
		twinEntityName = "vis_inactive_position",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityPosition.Fields.class)
//@CcpEntityOperations({
//		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeSaveFromMainEntity,  execute = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}, operationHandlers = {}),
//		// TODO THIS FLOW WILL BE REMOVED ALONG WITH THE GROUPINGS
//		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeDeleteFromMainEntity,  execute = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}, operationHandlers = {}),
//		//TODO REVISIT THIS FLOW
//		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeDeleteFromTwinEntity,  execute = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}, operationHandlers = {}),
//})

public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	public static enum MaxSalaryType { maxClt, maxPj }
	public static enum MinSalaryType { minClt, minPj }
	public static enum CltSalaryRange { maxClt, minClt }
	public static enum PjSalaryRange  { minPj, maxPj }
	
	@CcpJsonGlobalValidations(requiresAtLeastOne = {
			@CcpJsonValidationFieldList(MaxSalaryType.class),
			@CcpJsonValidationFieldList(MinSalaryType.class)
	}, requiresAllOrNone = {
			@CcpJsonValidationFieldList(CltSalaryRange.class),
			@CcpJsonValidationFieldList(PjSalaryRange.class)
	})
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionChannelTypes.class)
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
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionFrequencyTypes.class)
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
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionSortFieldTypes.class)
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

	/**
	 * Contact channels accepted by the {@code channel} field of the position entity, that is,
	 * where the recruiter wants to receive the resumes sent by the platform.
	 */
	public static enum VisPositionChannelTypes {

		telegram,
		whatsapp,
		email,
		sms
		;
	}

	/**
	 * Frequencies accepted by the {@code frequency} field of the position entity, that is,
	 * how often the matching resumes are sent to the recruiter.
	 */
	public static enum VisPositionFrequencyTypes {

		minute,
		hourly,
		daily,
		weekly,
		monthly
		;
	}

	/**
	 * Sorting criteria accepted by the {@code sortFields} field of the position entity,
	 * used to sort the resumes that will be presented to the recruiter.
	 */
	public static enum VisPositionSortFieldTypes {

		seniority,
		pj,
		clt,
		btc,
		disponibility,
		desiredSkills
		;
	}
}
