package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySaveFlow;
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
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeBefore;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.business.resume.VisBusinessExtractTextFromResume;
import com.vis.business.resume.VisBusinessNotifyResumeOwnerAboutEmptyResumeText;
import com.vis.business.resume.VisBusinessNotifyResumeOwnerAboutSuccessOnSavingHisResume;
import com.vis.business.resume.VisBusinessSaveResumeInBucket;
import com.vis.exceptions.VisBusinessErrorEmptyResumeText;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.utils.VisBusinessResumeSendToRecruiters;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityTwin(
		twinEntityName = "inactive_resume"
		,afterRecordBeenTransportedFromMainToTwinEntity = {} 
		,afterRecordBeenTransportedFromTwinToMainEntity = {}
		)
@CcpEntitySpecifications(
		flow = {
				@CcpEntitySaveFlow(whenThrowing = VisBusinessErrorEmptyResumeText.class, thenExecute = VisBusinessNotifyResumeOwnerAboutEmptyResumeText.class)
			   }, 
		afterSaveRecord = {VisBusinessSaveResumeInBucket.class, VisBusinessResumeSendToRecruiters.class, VisBusinessNotifyResumeOwnerAboutSuccessOnSavingHisResume.class},
		beforeSaveRecord = {VisBusinessExtractTextFromResume.class, VisBusinessExtractSkillsFromText.class},
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
		@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_000)
		clt, 
		@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_000)
		btc, 
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		companiesNotAllowed,  
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob, 
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(allowedValues = {"AUDITIVA", "VISUAL", "MOTORA", "MENTAL", "OUTRAS"})
		disabilities, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,
		@CcpJsonFieldTypeTimeBefore(maxValue = 70, intervalType = CcpEntityExpurgableOptions.yearly)
		experience, 
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		lastJob, 
		@CcpJsonFieldTypeNumber(maxValue = 100_000, minValue = 1_000)
		pj, 
		@CcpJsonFieldTypeNestedJson(jsonValidation = Skill.class)
		skill, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		;
	}
	
	enum Skill{
		
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		@CcpJsonFieldValidatorArray
		parent,
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		@CcpJsonFieldValidatorArray
		synonym,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		skill;
	}
}

