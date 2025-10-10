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
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.transformers.JnJsonTransformersDefaultEntityFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntitySpecifications(
		jsonValidation = VisEntitySkill.Fields.class,
		cacheableEntity = true, 
		afterSaveRecord = {},
		afterDeleteRecord = {} 
)
public class VisEntitySkill implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkill.class).entityInstance;
	
	public static enum Fields implements CcpEntityField{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		@CcpJsonFieldValidatorArray
		parent(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
		positionsCount(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = Word.class)
		@CcpJsonFieldValidatorArray
		prerequisite(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = Word.class)
		@CcpJsonFieldValidatorArray
		similar(false), 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill(true), 
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = Synonym.class)
		synonym(false),
		
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

enum Word{
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	word,
	@CcpJsonFieldTypeBoolean
	gemini,
	@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
	vagas;
}
enum Synonym{
	@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
	type,
	@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
	positionsCount;
}
