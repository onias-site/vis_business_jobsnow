package com.vis.entities;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityOperationSpecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTransferOperationEspecification;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;
import com.jn.json.fields.validation.JnJsonValidationsByFieldName;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.json.fields.validation.VisJsonValidationsByFieldName;

@CcpEntitySpecifications(
		classWithFieldsValidationsRules = VisEntityResumeLastView.Fields.class,
		inactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		reactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		delete = @CcpEntityOperationSpecification(afterOperation = {}),
	    save = @CcpEntityOperationSpecification(afterOperation = {}),
		cacheableEntity = true
)
public class VisEntityResumeLastView implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeLastView.class).entityInstance;

	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldValidator(type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
		recruiter(true), 
		@CcpJsonFieldValidator(required = true, validationsCatalog = {VisJsonValidationsByFieldName.class})
		email(true), 
		@CcpJsonFieldValidator(validationsCatalog = {JnJsonValidationsByFieldName.class})
		date(false), 
		@CcpJsonFieldValidator(validationsCatalog = {JnJsonValidationsByFieldName.class})
		timestamp(false), 
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Boolean)
		negativatedResume(false), 
		@CcpJsonFieldValidator(type = CcpJsonFieldType.Boolean)
		inactivePosition(false), 
		@CcpJsonFieldValidator(type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson//DOUBT DEVE-SE COLOCAR VALIDACAO DO TIPO NATIVO DESTE JSON?
		resume(false), 
		@CcpJsonFieldValidator(type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson//DOUBT DEVE-SE COLOCAR VALIDACAO DO TIPO NATIVO DESTE JSON?
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
