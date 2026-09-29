package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityOperation;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityOperations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityOperationType;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
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
import com.vis.business.resume.VisBusinessCalculateResumeHashes;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.utils.VisBusinessResumeSendToRecruiters;

/**
 * Represents the candidate's core Resume entity. Stores the complete professional profile:
 * desired contract type (CLT/PJ/BTC), availability, DDDs of interest, skills, experience, LinkedIn,
 * languages, company restrictions, seniority and time available for temporary work. Uses Twin Entity
 * to control inactive resumes, asynchronous writing, versioning and a 1-hour cache. On save or on
 * reactivation (deletion from the twin), it triggers the hash calculation and sends the resume to matching recruiters.
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8),})
@CcpEntityTwin(
		twinEntityName = "vis_inactive_resume",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		) 
@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityResume.Fields.class)
@CcpEntityOperations({
		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeSaveFromMainEntity,  execute = {VisBusinessCalculateResumeHashes.class, VisBusinessResumeSendToRecruiters.class}, operationHandlers = {}),
		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeDeleteFromTwinEntity,  execute = {VisBusinessResumeSendToRecruiters.class}, operationHandlers = {}),
})

public class VisEntityResume implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;

	public static enum SalaryType{
		pj, clt
	}
	
	@CcpJsonGlobalValidations(
			requiresAtLeastOne = {
			@CcpJsonValidationFieldList(SalaryType.class) 
	})
	public static enum Fields implements CcpJsonFieldName {

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

		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob,

		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		experience,

		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSkills.class)
		skill,

		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		lastJob,

		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		language,

		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(regexValidation = "^https://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-_%]+/?$")
		linkedinAddress,

		@CcpJsonFieldTypeBoolean
		negotiableClaim,

		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		notAllowedCompany,

		@CcpJsonFieldTypeBoolean
		pcd,

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		pj,

		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		temporallyJobTime,

		@CcpJsonFieldTypeBoolean
		travel,
		;
	}	
}

