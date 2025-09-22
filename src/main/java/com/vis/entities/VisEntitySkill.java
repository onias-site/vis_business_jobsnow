package com.vis.entities;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpEntityBulkOperationType;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityOperationSpecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTransferOperationEspecification;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;

@CcpEntitySpecifications(
		classWithFieldsValidationsRules = VisEntitySkill.Fields.class,
		inactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		reactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		delete = @CcpEntityOperationSpecification(afterOperation = {}),
	    save = @CcpEntityOperationSpecification(afterOperation = {}),
		cacheableEntity = true
)
public class VisEntitySkill implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkill.class).entityInstance;
	
	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		@CcpJsonFieldTypeArray
		parent(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(minValue = 1, integerNumber = true)
		positionsCount(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson(validationClass = Word.class)
		@CcpJsonFieldTypeArray
		prerequisite(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
		@CcpJsonFieldTypeNumber(minValue = 1, integerNumber = true)
		ranking(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson(validationClass = Word.class)
		@CcpJsonFieldTypeArray
		similar(false), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		skill(true), 
		@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.NestedJson)
		@CcpJsonFieldTypeNestedJson(validationClass = Synonym.class)
		@CcpJsonFieldTypeArray
		synonym(false)
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
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> collect = new CcpStringDecorator("C:\\eclipse-workspaces\\ccp\\jn\\jn-dependency-chooser-documentation-and-junit-testing\\documentation\\skills\\synonyms.json")
		.file().asJsonList().stream().map(json -> ENTITY.getMainBulkItem(json, CcpEntityBulkOperationType.create)).collect(Collectors.toList());
		;
		return collect;
	}
}

class Word{
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	Object word;
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Boolean)
	Object gemini;
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 1, integerNumber = true)
	Object vagas;
}
class Synonym{
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.String)
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	Object type;
	@CcpJsonFieldValidator(required = true, type = CcpJsonFieldType.Number)
	@CcpJsonFieldTypeNumber(minValue = 1, integerNumber = true)
	Object positionsCount;
}
