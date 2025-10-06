package com.vis.entities;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityDecorators;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityOperationSpecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTransferOperationEspecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTwin;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;
import com.ccp.json.validations.global.annotations.CcpJsonValidatorFieldList;
import com.ccp.json.validations.global.annotations.CcpJsonValidatorGlobal;
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.json.fields.validation.JnJsonValidationsByFieldName;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.business.resume.VisBusinessSaveResumeInBucket;
import com.vis.json.fields.validation.VisJsonValidationsByFieldName;
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
		@CcpJsonFieldValidator(validationsCatalog = {JnJsonValidationsByFieldName.class})
		date(false),// AUTOMATICO
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		ddd(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob(false), // VEM DO FRONT
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(allowedValues = {})//TODO QUAIS?
		@CcpJsonFieldTypeArray
		disabilities(false), // VEM DO FRONT
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		disponibility(false), // VEM DO FRONT
		@CcpJsonFieldValidator(required = true, validationsCatalog = {JnJsonValidationsByFieldName.class})
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
		@CcpJsonFieldValidator(validationsCatalog = {JnJsonValidationsByFieldName.class})
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
