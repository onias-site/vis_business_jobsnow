package com.vis.entities;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityDecorators;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTwin;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.business.resume.VisBusinessSaveResumeInBucket;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.utils.VisBusinessResumeSendToRecruiters;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(
		twinEntityName = "inactive_resume"
		,afterInactivate = {}, 
		 afterReactivate = {}
		)
@CcpEntitySpecifications(
		afterSaveRecord = {VisBusinessSaveResumeInBucket.class, VisBusinessResumeSendToRecruiters.class},
		jsonValidation = VisEntityResume.Fields.class,
		cacheableEntity = true, 
		afterDeleteRecord = {} 
)
@CcpJsonGlobalValidations(requiresAtLeastOne = {
		@CcpJsonValidationFieldList({"pj", "clt" })
})
public class VisEntityResume implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;
	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldTypeNumber(maxValue = 100000, minValue = 1000)
		clt(false), 
		@CcpJsonFieldTypeNumber(maxValue = 100000, minValue = 1000)
		btc(false), 
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		@CcpJsonFieldValidatorArray
		companiesNotAllowed(false),  
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date(false),
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob(false), 
		@CcpJsonFieldTypeString(allowedValues = {"AUDITIVA", "VISUAL", "MOTORA", "MENTAL", "OUTRAS"})
		@CcpJsonFieldValidatorArray
		disabilities(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email(true),
		@CcpJsonFieldTypeNumberUnsigned(maxValue = 70)
		experience(false), 
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		lastJob(false), 
		@CcpJsonFieldTypeNumber(maxValue = 100000, minValue = 1000)
		pj(false), 
		@CcpJsonFieldTypeNestedJson(jsonValidation = Skill.class)
		skill(false, VisBusinessExtractSkillsFromText.INSTANCE), 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp(false),
		;
		private final boolean primaryKey;

		private final Function<CcpJsonRepresentation, CcpJsonRepresentation> transformer;
		
		private Fields(boolean primaryKey) {
			this(primaryKey, CcpOtherConstants.DO_NOTHING);
		}

		private Fields(boolean primaryKey, Function<CcpJsonRepresentation, CcpJsonRepresentation> transformer) {
			this.transformer = transformer;
			this.primaryKey = primaryKey;
		}
		
		public Function<CcpJsonRepresentation, CcpJsonRepresentation> getTransformer() {
			return this.transformer == CcpOtherConstants.DO_NOTHING ? JnJsonTransformersDefaultEntityFields.getTransformer(this) : this.transformer;
		}

		public boolean isPrimaryKey() {
			return this.primaryKey;
		}
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

