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
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
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
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.business.position.VisBusinessDuplicateFieldEmailToFieldMasters;
import com.vis.business.position.VisBusinessGroupPositionsGroupedByRecruiters;
import com.vis.business.resume.VisBusinessExtractSkillsFromText;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;
import com.vis.utils.VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(
		twinEntityName = "inactive_position"
		,afterReactivate = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class},
		afterInactivate = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}
		)
@CcpEntitySpecifications(
		afterSaveRecord = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class},
		jsonValidation = VisEntityPosition.Fields.class,
		cacheableEntity = true, 
		afterDeleteRecord = {} 
)
@CcpJsonGlobalValidations(requiresAtLeastOne = {
		@CcpJsonValidationFieldList({"maxClt", "maxPj" }),
		@CcpJsonValidationFieldList({"minClt", "minPj" })
}, requiresAllOrNone = {
		@CcpJsonValidationFieldList({"maxClt", "minClt" }),
		@CcpJsonValidationFieldList({"minPj", "maxPj" })
})
public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValues = {  "telegram", "whatsapp", "email", "sms" })
		@CcpJsonFieldValidatorArray
		channel(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
		contactChannel(false), 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date(false),
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10000)
		description(false, VisBusinessExtractSkillsFromText.INSTANCE), 
		@CcpJsonFieldTypeNestedJson
		@CcpJsonFieldValidatorArray
		desiredSkill(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email(true, VisJsonTransformerPutEmailHashAndDomainRecruiter.INSTANCE), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeTimeBefore(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.yearly)
		expireDate(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValues = { "minute", "hourly", "daily", "weekly", "monthly" })
		frequency(false), 
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		pcd(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		requiredSkill(false), 
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		seniority(true), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValues = { "seniority", "pj", "clt", "btc", "disponibility", "desiredSkills" })
		@CcpJsonFieldValidatorArray(minSize = 1)
		sortFields(false), 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		title(true), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeBoolean
		showSalaryExpectation(false),
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minBtc(false),
		@CcpJsonFieldTypeNumber(maxValue = 100000)
		maxBtc(false),
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minClt(false),
		@CcpJsonFieldTypeNumber(maxValue = 100000)
		maxClt(false),
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minPj(false),
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
