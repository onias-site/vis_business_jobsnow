package com.vis.entities;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntitySpecifications(
		jsonValidation = VisEntityResumeLastView.Fields.class,
		cacheableEntity = true, 
		afterSaveRecord = {},
		afterDeleteRecord = {} 
)
public class VisEntityResumeLastView implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeLastView.class).entityInstance;

	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
		recruiter(true), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		email(true), 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date(false), 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp(false),
		@CcpJsonFieldTypeBoolean
		negativatedResume(false),
		@CcpJsonFieldTypeBoolean
		inactivePosition(false),
		@CcpJsonFieldTypeNestedJson(validationClass = VisEntityResume.Fields.class)
		resume(false), 
		@CcpJsonFieldTypeNestedJson(validationClass = VisEntityPosition.Fields.class)
		position(false)
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
