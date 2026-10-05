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

	/** The entity {@code vis_position}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	/** The maximum salary fields; a position needs at least one of them. */
	public static enum MaxSalaryType {
		/** The maximum salary as an employee (CLT). */
		maxClt,
		/** The maximum value as a contractor (PJ). */
		maxPj }
	/** The minimum salary fields; a position needs at least one of them. */
	public static enum MinSalaryType {
		/** The minimum salary as an employee (CLT). */
		minClt,
		/** The minimum value as a contractor (PJ). */
		minPj }
	/** The salary range as an employee (CLT): the minimum may not exceed the maximum. */
	public static enum CltSalaryRange {
		/** The maximum salary as an employee. */
		maxClt,
		/** The minimum salary as an employee. */
		minClt }
	/** The value range as a contractor (PJ): the minimum may not exceed the maximum. */
	public static enum PjSalaryRange  {
		/** The minimum value as a contractor. */
		minPj,
		/** The maximum value as a contractor. */
		maxPj }
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	@CcpJsonGlobalValidations(requiresAtLeastOne = {
			@CcpJsonValidationFieldList(MaxSalaryType.class),
			@CcpJsonValidationFieldList(MinSalaryType.class)
	}, requiresAllOrNone = {
			@CcpJsonValidationFieldList(CltSalaryRange.class),
			@CcpJsonValidationFieldList(PjSalaryRange.class)
	})
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code channel} field: required, text, list. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionChannelTypes.class)
		@CcpJsonFieldValidatorArray
		channel, 
		/** The {@code contactChannel} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
		contactChannel, 
		/** The {@code date} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		/** The {@code ddd} field: required, list, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd, 
		/** The {@code description} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10_000)
		description, 
		/** The {@code desiredSkill} field: nested JSON, list. */
		@CcpJsonFieldTypeNestedJson
		@CcpJsonFieldValidatorArray
		desiredSkill, 
		/** The {@code disponibility} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility, 
		/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}, transformed by {@code VisJsonTransformerPutEmailHashAndDomainRecruiter}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerPutEmailHashAndDomainRecruiter.class)
		email, 
		/** The {@code expireDate} field: required, past timestamp. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeTimeBefore(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.yearly)
		expireDate, 
		/** The {@code frequency} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionFrequencyTypes.class)
		frequency, 
		/** The {@code pcd} field: boolean. */
		@CcpJsonFieldTypeBoolean
		pcd, 
		/** The {@code requiredSkill} field: required, list, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		requiredSkill, 
		/** The {@code seniority} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		seniority, 
		/** The {@code sortFields} field: required, text, list. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionSortFieldTypes.class)
		@CcpJsonFieldValidatorArray(minSize = 1)
		sortFields, 
		/** The {@code timestamp} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp, 
		/** The {@code title} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		title, 
		/** The {@code showSalaryExpectation} field: required, boolean. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeBoolean
		showSalaryExpectation,
		/** The {@code minBtc} field: decimal number. */
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minBtc,
		/** The {@code maxBtc} field: decimal number. */
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxBtc,
		/** The {@code minClt} field: decimal number. */
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minClt,
		/** The {@code maxClt} field: decimal number. */
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxClt,
		/** The {@code minPj} field: decimal number. */
		@CcpJsonFieldTypeNumber(minValue = 1_000)
		minPj,
		/** The {@code maxPj} field: decimal number. */
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxPj,
		;
	}

	/**
	 * Contact channels accepted by the {@code channel} field of the position entity, that is,
	 * where the recruiter wants to receive the resumes sent by the platform.
	 */
	public static enum VisPositionChannelTypes {

		/** Telegram. */
		telegram,
		/** WhatsApp. */
		whatsapp,
		/** E-mail. */
		email,
		/** SMS. */
		sms
		;
	}

	/**
	 * Frequencies accepted by the {@code frequency} field of the position entity, that is,
	 * how often the matching resumes are sent to the recruiter.
	 */
	public static enum VisPositionFrequencyTypes {

		/** Every minute. */
		minute,
		/** Every hour. */
		hourly,
		/** Every day. */
		daily,
		/** Every week. */
		weekly,
		/** Every month. */
		monthly
		;
	}

	/**
	 * Sorting criteria accepted by the {@code sortFields} field of the position entity,
	 * used to sort the resumes that will be presented to the recruiter.
	 */
	public static enum VisPositionSortFieldTypes {

		/** By seniority. */
		seniority,
		/** By the value as a contractor (PJ). */
		pj,
		/** By the salary as an employee (CLT). */
		clt,
		/** By the value in bitcoin. */
		btc,
		/** By availability. */
		disponibility,
		/** By the desired skills found in the resume. */
		desiredSkills
		;
	}
}
