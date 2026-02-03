package com.vis.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntitySkill.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
public class VisEntitySkill implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkill.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill, 
		
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking,

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		parent,
	
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,
		;
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		var synonyms = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json")
		.file()
		.asJsonList()
		;
		List<String> lines = new CcpStringDecorator("C:\\logs\\skills\\countByWords.txt")
				 .file().getLines();
		
		List<CcpJsonRepresentation> collect = new ArrayList<>(synonyms.stream().map(json -> {
			int resumesCount = this.getResumesCount(json, lines);
			
			CcpJsonRepresentation put = json.getDynamicVersion().put("resumesCount", resumesCount);
			
			return put;
			
		}).collect(Collectors.toList()));
		
		
		collect.sort((a, b) -> b.getDynamicVersion().getAsIntegerNumber("resumesCount") - a.getDynamicVersion().getAsIntegerNumber("resumesCount"));
		
		int ranking = 1;
		
		List<CcpBulkItem> response = new ArrayList<>();
		
		for (CcpJsonRepresentation json : collect) {
			CcpJsonRepresentation jsonPiece = json.getJsonPiece(Fields.values());
			CcpJsonRepresentation put = jsonPiece.put(Fields.ranking, ranking++);
			CcpBulkItem mainBulkItem = ENTITY.getMainBulkItem(put, CcpBulkEntityOperationType.create);
			response.add(mainBulkItem);
		}
		
		return response;
	}
	
	private int getResumesCount(CcpJsonRepresentation json, List<String> lines) {
		String skill = json.getAsString(Fields.skill);
		List<String> synonym = json.getAsStringList(Fields.synonym);
		Set<String> skills = new HashSet<>(synonym);
		skills.add(skill);
		
		 int total = 0;
		 
		 for (String word : skills) {
			 
			 String start = word + " = ";
			
			 Integer orElse = new ArrayList<>(lines).stream().filter(line -> line.startsWith(start)).map(line -> line.replace(start, "").trim())
			 .map(line -> Integer.valueOf(line))
			 .findFirst()
			 .orElse(0);
			 
			 total += orElse;
		}
		 
		return total;
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
