
package com.vis.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
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
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials;

@CcpEntityCache(3600)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntityGroupPositionsBySkills.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
public class VisEntityGroupPositionsBySkills implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityGroupPositionsBySkills.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(exactLength = 2)
		firstTwoInitials, 
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials.class)
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldValidatorRequired
		skill,
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		;
	}
	
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		var synonyms = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json")
		.file()
		.asJsonList();
		var wordsAndSkills = new HashMap<String, String>();
		for (var synonym : synonyms) {
			
			List<String> parents = synonym.getDynamicVersion().getAsStringList("parent");
			for (var parent : parents) {
				wordsAndSkills.put(parent, parent);
			}
			
			String skill = synonym.getDynamicVersion().getAsString("skill").toUpperCase();
			wordsAndSkills.put(skill, skill);
			{
				List<CcpJsonRepresentation> words = synonym.getDynamicVersion().getAsJsonList("synonym");
				for (var word : words) {
					String upperCase = word.getDynamicVersion().getAsString("skill").toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getDynamicVersion().getAsJsonList("preRequisite");
				for (var word : words) {
					String upperCase = word.getDynamicVersion().getAsString("word").toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
		}
		CcpJsonRepresentation groupedSkills = CcpOtherConstants.EMPTY_JSON;
		Set<String> words = wordsAndSkills.keySet();
		
		for (String word : words) {
			if(word.length() < 2) {
				continue;
			}

			if(word.length() > 35) {
				continue;
			}
			
			String initials = word.substring(0, 2);
			String skill = wordsAndSkills.get(word);
			List<CcpJsonRepresentation> asJsonList = groupedSkills.getDynamicVersion()
			.getAsJsonList(initials);
			
			ArrayList<CcpJsonRepresentation> arrayList = new ArrayList<>(asJsonList);

			CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON.getDynamicVersion()
					.put("skill", skill)
					.getDynamicVersion()
					.put("word", word);
			arrayList.add(json);
			
			groupedSkills = groupedSkills.getDynamicVersion().put(initials, arrayList);
		}
		CcpJsonRepresentation groupedSkills2 = new CcpJsonRepresentation(groupedSkills.content);
		List<CcpBulkItem> collect = groupedSkills.fieldSet().stream()
		.map(initials -> {
			List<CcpJsonRepresentation> skill = groupedSkills2.getDynamicVersion().getAsJsonList(initials);
			CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityGroupPositionsBySkills.Fields.skill, skill)
					.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, initials)
					;
			return json
		;
		})
		.map(json -> new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY))
		.collect(Collectors.toList());
		
		
		return collect;
	}	
}
