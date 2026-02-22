package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.entities.decorators.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.business.resume.VisBusinessCalculateResumeHashes;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
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
		@CcpJsonFieldTypeString(minLength = 5, maxLength = 100)
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

