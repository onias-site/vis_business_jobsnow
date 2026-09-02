
package com.vis.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import java.util.stream.Stream;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.decorators.CcpFileDecorator;

/**
 * Representa o índice de habilidades (skills) agrupadas pelas duas primeiras letras da palavra,
 * utilizado como dicionário de lookup para matching de skills em textos. Também contém lógica de
 * carga inicial a partir de arquivo de sinônimos. Possui cache de 1 hora.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityGroupPositionsBySkills.Fields.class)
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
		date
		;
	}
	
	private Set<String> getAllParents(List<CcpJsonRepresentation>synonyms, String word, Set<String> allParents){
		Stream<CcpJsonRepresentation> stream = synonyms.stream();
		var filter = stream
		.filter(x -> x.getAsString(VisJsonCommonsFields.skill).equals(word) ||
		             x.getAsJsonList(VisJsonCommonsFields.synonym).stream().anyMatch(y -> y.getAsString(VisJsonCommonsFields.skill).equals(word)));

		             Optional<CcpJsonRepresentation> findFirst = filter
		.findFirst();
		boolean findFirstPresent = findFirst.isPresent();

		boolean parentNotFound = false == findFirstPresent;
		
		if(parentNotFound) {
			return allParents;
		}
		
		CcpJsonRepresentation synonym = findFirst.get();
		boolean containsAllFields = synonym.containsAllFields(VisJsonCommonsFields.parent);

		boolean parentAbsent = false == containsAllFields;
		if(parentAbsent) {
			return allParents;
		}
		
		List<String> parent = synonym.getAsStringList(VisJsonCommonsFields.parent);
		allParents.addAll(parent);
		
		return allParents;
	}
	
	
	public static int getWordStatus(String word) {
		String upperCase = word.toUpperCase();
		String firstTwoInitials = upperCase.substring(0,2);
		CcpJsonRepresentation id = CcpOtherConstants.EMPTY_JSON.put(Fields.firstTwoInitials, firstTwoInitials);
		CcpEntityMetaData entityMetaData = ENTITY.getEntityMetaData();
		CcpJsonRepresentation oneById = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(id, json -> CcpOtherConstants.EMPTY_JSON);
		
		boolean notFound = oneById.isEmpty();
		
		if(notFound) {
			return 1;
		}
		
		
		List<CcpJsonRepresentation> skills = oneById.getAsJsonList(VisJsonCommonsFields.skill);
		for (CcpJsonRepresentation skill : skills) {
			{
				String wrd = skill.getAsString(VisJsonCommonsFields.word);
				boolean wrdEquals = wrd.equals(upperCase);
				if(wrdEquals) {
					return 0;
				}
			}
			{
				String wrd = skill.getAsString(VisJsonCommonsFields.skill);
				boolean wrdEquals2 = wrd.equals(upperCase);
				if(wrdEquals2) {
					return 0;
				}
			}
		}
		
		return 2;
	} 
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json");
		CcpFileDecorator ccpStringDecoratorFile = ccpStringDecorator
		.file();
		var synonyms = ccpStringDecoratorFile
		.asJsonList();
		
		var wordsAndParents = new HashMap<String, Set<String>>();
		var wordsAndSkills = new HashMap<String, String>();

		for (CcpJsonRepresentation synonym : synonyms) {
			
			List<String> parents = synonym.getAsStringList(VisJsonCommonsFields.parent);

			Set<String> allParents = new HashSet<String>();
			allParents.addAll(parents);

			for (var parent : parents) {
				wordsAndSkills.put(parent, parent);
				allParents = this.getAllParents(synonyms, parent, allParents);
			}
			List<String> allNames = new ArrayList<>();
			String mainName = synonym.getAsString(VisJsonCommonsFields.skill);
			List<CcpJsonRepresentation> asJsonList2 = synonym.getAsJsonList(VisJsonCommonsFields.synonym);
			Stream<CcpJsonRepresentation> stream2 = asJsonList2.stream();
			var stream2Map = stream2.map(x -> x.getAsString(VisJsonCommonsFields.skill));
			List<String> otherNames = stream2Map.collect(Collectors.toList());
			allNames.add(mainName);
			allNames.addAll(otherNames);
			for (var name : allNames) {
				wordsAndParents.put(name, allParents);
			}
			String asString = synonym.getAsString(VisJsonCommonsFields.skill);

			String skill = asString.toUpperCase();
			wordsAndSkills.put(skill, skill);
			{
				List<CcpJsonRepresentation> words = synonym.getAsJsonList(VisJsonCommonsFields.synonym);
				for (CcpJsonRepresentation word : words) {
					String asString2 = word.getAsString(VisJsonCommonsFields.skill);
					String upperCase = asString2.toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getAsJsonList(JsonFields.preRequisite);
				for (CcpJsonRepresentation word : words) {
					String asString3 = word.getAsString(VisJsonCommonsFields.word);
					String upperCase = asString3.toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getAsJsonList(JsonFields.similar);
				for (CcpJsonRepresentation word : words) {
					String asString4 = word.getAsString(VisJsonCommonsFields.word);
					String toUpperCase = asString4.toUpperCase();
					String upperCase = toUpperCase.replace("_", " ");
					wordsAndSkills.put(upperCase, skill);
				}
			}
		}
		CcpJsonRepresentation groupedSkills = CcpOtherConstants.EMPTY_JSON;
		Set<String> words = wordsAndSkills.keySet();
		
		for (String word : words) {
			int wordLength = word.length();
			boolean wordLengthMenor = wordLength < 2;
			if(wordLengthMenor) {
				continue;
			}
			int wordLength2 = word.length();
			boolean wordLength2Maior = wordLength2 > 50;

			if(wordLength2Maior) {
				continue;
			}
			
			String initials = word.substring(0, 2);
			String skill = wordsAndSkills.get(word);
			CcpFieldName ccpFieldName = new CcpFieldName(initials);
			List<CcpJsonRepresentation> asJsonList = groupedSkills.getAsJsonList(ccpFieldName);

			ArrayList<CcpJsonRepresentation> arrayList = new ArrayList<>(asJsonList);
			Set<String> parent = wordsAndParents.getOrDefault(word, new HashSet<>());
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.skill, skill);
					CcpJsonRepresentation put2 = put
					.put(VisJsonCommonsFields.word, word);
					CcpJsonRepresentation json = put2
					.put(VisJsonCommonsFields.parent, parent)
					;
			arrayList.add(json);
			CcpFieldName ccpFieldName2 = new CcpFieldName(initials);

			groupedSkills = groupedSkills.put(ccpFieldName2, arrayList);
		}
		CcpJsonRepresentation groupedSkills2 = new CcpJsonRepresentation(groupedSkills.content);
		Set<String> fieldSet = groupedSkills.fieldSet();
		Stream<String> stream3 = fieldSet.stream();
		var stream3Map = stream3
		.map(initials -> {
			CcpFieldName ccpFieldName3 = new CcpFieldName(initials);
			List<CcpJsonRepresentation> skill = groupedSkills2.getAsJsonList(ccpFieldName3);
			CcpJsonRepresentation put3 = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.skill, skill);
					CcpJsonRepresentation json = put3
					.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, initials)
					;
			return json
		;
		});
		var stream3MapMap = stream3Map
		.map(json -> new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY, ENTITY.calculateId(json)));
		List<CcpBulkItem> collect = stream3MapMap
		.collect(Collectors.toList());
		
		
		return collect;
	}	
	
	static enum JsonFields implements CcpJsonFieldName{ similar, preRequisite}
}
