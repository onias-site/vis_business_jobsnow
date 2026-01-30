
package com.vis.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
	
	private Set<String> getAllParents(List<CcpJsonRepresentation>synonyms, String word, Set<String> allParents){
		
		Optional<CcpJsonRepresentation> findFirst = synonyms.stream()
		.filter(x -> x.getDynamicVersion().getAsString("skill").equals(word) || 
		             x.getDynamicVersion().getAsJsonList("synonym").stream().anyMatch(y -> y.getDynamicVersion().getAsString("skill").equals(word)))
		.findFirst();	
		
		boolean parentNotFound = false == findFirst.isPresent();
		
		if(parentNotFound) {
			return allParents;
		}
		
		CcpJsonRepresentation synonym = findFirst.get();
		
		boolean parentAbsent = false == synonym.getDynamicVersion().containsAllFields("parent");
		if(parentAbsent) {
			return allParents;
		}
		
		List<String> parent = synonym.getDynamicVersion().getAsStringList("parent");
		allParents.addAll(parent);
		
		return allParents;
	}
	
	
	public static int getWordStatus(String word) {
		String upperCase = word.toUpperCase();
		String firstTwoInitials = upperCase.substring(0,2);
		CcpJsonRepresentation id = CcpOtherConstants.EMPTY_JSON.put(Fields.firstTwoInitials, firstTwoInitials);
		CcpJsonRepresentation oneById = ENTITY.getOneByIdOrHandleItIfThisIdWasNotFound(id, json -> CcpOtherConstants.EMPTY_JSON);
		
		boolean notFound = oneById.isEmpty();
		
		if(notFound) {
			return 1;
		}

		
		
		List<CcpJsonRepresentation> skills = oneById.getAsJsonList(Fields.skill);
		for (CcpJsonRepresentation skill : skills) {
			{
				String wrd = skill.getAsString(VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials.word);
				if(wrd.equals(upperCase)) {
					return 0;
				}
			}
			{
				String wrd = skill.getAsString(VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials.skill);
				if(wrd.equals(upperCase)) {
					return 0;
				}
			}
		}
		
		return 2;
	} 
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		var synonyms = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json")
		.file()
		.asJsonList();
		
		var wordsAndParents = new HashMap<String, Set<String>>();
		var wordsAndSkills = new HashMap<String, String>();

		for (CcpJsonRepresentation synonym : synonyms) {
			
			List<String> parents = synonym.getDynamicVersion().getAsStringList("parent");
			
			Set<String> allParents = new HashSet<String>();
			allParents.addAll(parents);

			
			for (var parent : parents) {
				wordsAndSkills.put(parent, parent);
				allParents = this.getAllParents(synonyms, parent, allParents);
			}
			List<String> allNames = new ArrayList<>();
			String mainName = synonym.getDynamicVersion().getAsString("skill");
			List<String> otherNames = synonym.getDynamicVersion().getAsJsonList("synonym").stream().map(x -> x.getDynamicVersion().getAsString("skill")).collect(Collectors.toList());
			allNames.add(mainName);
			allNames.addAll(otherNames);
			for (var name : allNames) {
				wordsAndParents.put(name, allParents);
			}
			
			String skill = synonym.getDynamicVersion().getAsString("skill").toUpperCase();
			wordsAndSkills.put(skill, skill);
			{
				List<CcpJsonRepresentation> words = synonym.getDynamicVersion().getAsJsonList("synonym");
				for (CcpJsonRepresentation word : words) {
					String upperCase = word.getDynamicVersion().getAsString("skill").toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getDynamicVersion().getAsJsonList("preRequisite");
				for (CcpJsonRepresentation word : words) {
					String upperCase = word.getDynamicVersion().getAsString("word").toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getDynamicVersion().getAsJsonList("similar");
				for (CcpJsonRepresentation word : words) {
					String upperCase = word.getDynamicVersion().getAsString("word").toUpperCase().replace("_", " ");
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
			Set<String> parent = wordsAndParents.getOrDefault(word, new HashSet<>());
			CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON.getDynamicVersion()
					.put("skill", skill)
					.getDynamicVersion()
					.put("word", word)
					.getDynamicVersion()
					.put("parent", parent)
					;
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
