package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeBefore;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.business.resume.VisBusinessCalculateResumeHashes;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.utils.VisBusinessResumeSendToRecruiters;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityTwin(
		twinEntityName = "inactive_resume"
		,afterRecordBeenTransportedFromMainToTwinEntity = {} 
		,afterRecordBeenTransportedFromTwinToMainEntity = {VisBusinessResumeSendToRecruiters.class}
		)
@CcpEntitySpecifications(
		flow = {
			   }, 
		afterSaveRecord = {VisBusinessCalculateResumeHashes.class, VisBusinessResumeSendToRecruiters.class},
		beforeSaveRecord = {},
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntityResume.Fields.class,
		afterDeleteRecord = {} 
)
@CcpJsonGlobalValidations(
		requiresAtLeastOne = {
		@CcpJsonValidationFieldList({"pj", "clt" })
})
public class VisEntityResume implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_500)
		clt, 
		@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_000)
		btc, 
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		notAllowedCompany,  
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeTimeBefore(maxValue = 70, intervalType = CcpEntityExpurgableOptions.yearly)
		experience, 
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		lastJob, 
		@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 2_500)
		pj, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		@CcpJsonFieldTypeBoolean
		pcd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		temporallyJobTime,
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		language,
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		@CcpJsonFieldValidatorArray
		includedSkill,
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		@CcpJsonFieldValidatorArray
		excludedSkill,
		@CcpJsonFieldTypeBoolean
		negotiableClaim, 
		@CcpJsonFieldTypeString(minLength = 5, maxLength = 100)
		linkedinAddress, 
		@CcpJsonFieldTypeBoolean
		travel, 
		;
	}
	
}

