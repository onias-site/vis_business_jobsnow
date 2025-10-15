package com.vis.entities;

import java.util.List;
import java.util.stream.Collectors;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntitySkill.Fields.class,
		cacheableEntity = true, 
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
public class VisEntitySkill implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkill.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		@CcpJsonFieldValidatorArray
		parent, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned(minValue = 1)
		positionsCount, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = Word.class)
		@CcpJsonFieldValidatorArray
		prerequisite, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = Word.class)
		@CcpJsonFieldValidatorArray
		similar, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill, 
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = Synonym.class)
		synonym,
		;
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> collect = new CcpStringDecorator("C:\\eclipse-workspaces\\ccp\\jn\\jn-dependency-chooser-documentation-and-junit-testing\\documentation\\skills\\synonyms.json")
		.file().asJsonList().stream().map(json -> ENTITY.getMainBulkItem(json, CcpBulkEntityOperationType.create)).collect(Collectors.toList());
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
