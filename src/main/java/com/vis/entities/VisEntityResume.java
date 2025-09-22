package com.vis.entities;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityDecorators;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityOperationSpecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTransferOperationEspecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTwin;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTime;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;
import com.ccp.json.validations.global.annotations.CcpJsonValidatorFieldList;
import com.ccp.json.validations.global.annotations.CcpJsonValidatorGlobal;
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.business.resume.VisBusinessSaveResumeInBucket;
import com.vis.utils.VisBusinessResumeSendToRecruiters;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(twinEntityName = "inactive_resume")
@CcpEntitySpecifications(
		classWithFieldsValidationsRules = VisEntityResume.Fields.class,
		inactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		reactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		delete = @CcpEntityOperationSpecification(afterOperation = {}),
	    save = @CcpEntityOperationSpecification(afterOperation = {VisBusinessSaveResumeInBucket.class, VisBusinessResumeSendToRecruiters.class }),
		cacheableEntity = true
)
@CcpJsonValidatorGlobal(requiresAtLeastOne = {
		@CcpJsonValidatorFieldList({"pj", "clt" })
})
public class VisEntityResume implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;
	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 100000, minValue = 1000)
		clt(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 100000, minValue = 1000)
		btc(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		@CcpJsonFieldTypeArray
		companiesNotAllowed(false), // VEM DO FRONT 
		date(false),// AUTOMATICO
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeArray(minSize = 1)
		@CcpJsonFieldTypeNumber(allowedValues = { 10, 61, 62, 64, 65, 66, 67, 82, 71, 73, 74, 75,
				77, 85, 88, 98, 99, 83, 81, 87, 86, 89, 84, 79, 68, 96, 92, 97,
				91, 93, 94, 69, 95, 63, 27, 28, 31, 32, 33, 34, 35, 37, 38, 21,
				22, 24, 11, 12, 13, 14, 15, 16, 17, 18, 19, 41, 42, 43, 44, 45,
				46, 51, 53, 54	, 55, 47, 48, 49 })
		ddd(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(allowedValues = {})//TODO QUAIS?
		@CcpJsonFieldTypeArray
		disabilities(false), // VEM DO FRONT
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.TimeAfterCurrentDate)
		@CcpJsonFieldTypeTime(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.monthly)
		disponibility(false), // VEM DO FRONT
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(regexValidation = CcpEmailDecorator.EMAIL_REGEX, minLength = 7, maxLength = 100)
		email(true),// VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 70, minValue = 0)
		experience(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		lastJob(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 100000, minValue = 1000)
		pj(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson()//TODO QUAL VALIDACAO?
		skill(false, VisBusinessExtractSkillsFromText.INSTANCE), // CALCULADO
		timestamp(false),//AUTOMATICO
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
}
