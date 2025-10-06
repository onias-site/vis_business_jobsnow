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
import com.jn.json.fields.validation.JnJsonValidationsByFieldName;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.business.position.VisBusinessDuplicateFieldEmailToFieldMasters;
import com.vis.business.position.VisBusinessGroupPositionsGroupedByRecruiters;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.json.fields.validation.VisJsonValidationsByFieldName;
import com.vis.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;
import com.vis.utils.VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(twinEntityName = "inactive_position")
@CcpEntitySpecifications(
		classWithFieldsValidationsRules = VisEntityPosition.Fields.class,
		inactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		reactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		delete = @CcpEntityOperationSpecification(afterOperation = {}),
	    save = @CcpEntityOperationSpecification(afterOperation = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}),
		cacheableEntity = true
)

@CcpJsonValidatorGlobal(requiresAtLeastOne = {
		@CcpJsonValidatorFieldList({"maxClt", "maxPj" }),
		@CcpJsonValidatorFieldList({"minClt", "minPj" })
}, requiresAllOrNone = {
		@CcpJsonValidatorFieldList({"maxClt", "minClt" }),
		@CcpJsonValidatorFieldList({"minPj", "maxPj" })
})
//FIXME MUDAR TABELA
public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(allowedValues = {  "telegram", "whatsapp", "email", "sms" })
		@CcpJsonFieldTypeArray
		channel(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
		contactChannel(false), 
		@CcpJsonFieldValidator(validationsCatalog = {JnJsonValidationsByFieldName.class})
		date(false),
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		ddd(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10000)
		description(false, VisBusinessExtractSkillsFromText.INSTANCE), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson
		@CcpJsonFieldTypeArray
		desiredSkill(false), 
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		disponibility(false), 
		@CcpJsonFieldValidator(required = true, validationsCatalog = {JnJsonValidationsByFieldName.class})
		email(true, VisJsonTransformerPutEmailHashAndDomainRecruiter.INSTANCE), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.TimeAfterCurrentDate)
		@CcpJsonFieldTypeTime(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.yearly)
		expireDate(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(allowedValues = { "minute", "hourly", "daily", "weekly", "monthly" })
		frequency(false), 
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		pcd(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		requiredSkill(false), 
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		seniority(true), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(allowedValues = { "seniority", "pj", "clt", "btc", "disponibility", "desiredSkills" })
		@CcpJsonFieldTypeArray(minSize = 1)
		sortFields(false), 
		@CcpJsonFieldValidator(validationsCatalog = {JnJsonValidationsByFieldName.class})
		timestamp(false), 
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		title(true), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Boolean)
		showSalaryExpectation(false),
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minBtc(false),
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 100000)
		maxBtc(false),
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minClt(false),
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 100000)
		maxClt(false),
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minPj(false),

		@CcpJsonFieldValidator(type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(maxValue = 100000)
		maxPj(false),
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
