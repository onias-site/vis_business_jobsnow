
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
 * Represents the index of skills grouped by the first two letters of the word, used as a lookup
 * dictionary to match skills in texts. Also contains the initial load logic from the synonyms file.
 * Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityGroupPositionsBySkills.Fields.class)
public class VisEntityGroupPositionsBySkills implements CcpEntityConfigurator {

	/** The entity {@code vis_group_positions_by_skills}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityGroupPositionsBySkills.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code firstTwoInitials} field: part of the primary key, text. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(exactLength = 2)
		firstTwoInitials, 
		/** The {@code skill} field: nested JSON, list, required. */
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSkillsGroupedByTheirTwoFirstInitials.class)
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldValidatorRequired
		skill,
		/** The {@code timestamp} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		/** The {@code date} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date
		;
	}
	
	/**
	 * Adds the parents of the word: the parents of the first synonym record whose skill, or one of its synonyms, is the
	 * word (one level only).
	 * @param synonyms the synonym records
	 * @param word the word
	 * @param allParents the parents found so far
	 * @return the parents found so far plus the ones of the word
	 */
	private Set<String> getAllParents(List<CcpJsonRepresentation>synonyms, String word, Set<String> allParents){
		Stream<CcpJsonRepresentation> synonymsStream = synonyms.stream();
		var synonymsMatchingWord = synonymsStream
		.filter(x -> x.getAsString(VisJsonCommonsFields.skill).equals(word) ||
		             x.getAsJsonList(VisJsonCommonsFields.synonym).stream().anyMatch(y -> y.getAsString(VisJsonCommonsFields.skill).equals(word)));

		             Optional<CcpJsonRepresentation> firstMatchingSynonym = synonymsMatchingWord
		.findFirst();
		boolean synonymFound = firstMatchingSynonym.isPresent();

		boolean parentNotFound = false == synonymFound;
		
		if(parentNotFound) {
			return allParents;
		}
		
		CcpJsonRepresentation synonym = firstMatchingSynonym.get();
		boolean hasParent = synonym.containsAllFields(VisJsonCommonsFields.parent);

		boolean parentAbsent = false == hasParent;
		if(parentAbsent) {
			return allParents;
		}
		
		List<String> parent = synonym.getAsStringList(VisJsonCommonsFields.parent);
		allParents.addAll(parent);
		
		return allParents;
	}
	
	
	/**
	 * Tells whether a word is a known skill, by the group of its first two letters (upper case).
	 * @param word the word, with at least two characters
	 * @return 0 when the word (or skill) is in the group, 1 when there is no group for its initials, 2 when the group exists
	 * but does not have it
	 */
	public static int getWordStatus(String word) {
		String upperCase = word.toUpperCase();
		String firstTwoInitials = upperCase.substring(0,2);
		CcpJsonRepresentation id = CcpOtherConstants.EMPTY_JSON.put(Fields.firstTwoInitials, firstTwoInitials);
		CcpEntityMetaData entityMetaData = ENTITY.getEntityMetaData();
		CcpJsonRepresentation skillsGroup = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(id, json -> CcpOtherConstants.EMPTY_JSON);
		
		boolean notFound = skillsGroup.isEmpty();
		
		if(notFound) {
			return 1;
		}
		
		
		List<CcpJsonRepresentation> skills = skillsGroup.getAsJsonList(VisJsonCommonsFields.skill);
		for (CcpJsonRepresentation skill : skills) {
			{
				String skillWord = skill.getAsString(VisJsonCommonsFields.word);
				boolean wordMatches = skillWord.equals(upperCase);
				if(wordMatches) {
					return 0;
				}
			}
			{
				String skillName = skill.getAsString(VisJsonCommonsFields.skill);
				boolean skillMatches = skillName.equals(upperCase);
				if(skillMatches) {
					return 0;
				}
			}
		}
		
		return 2;
	} 
	/**
	 * Seeds the groups from {@code documentation/jn/skills/synonyms.json} (relative to the working directory): every skill,
	 * synonym, prerequisite and similar word (upper case, 2 to 50 characters) becomes an item with its main skill and
	 * parents, grouped by its first two letters.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpStringDecorator synonymsFilePath = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json");
		CcpFileDecorator synonymsFile = synonymsFilePath
		.file();
		var synonyms = synonymsFile
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
			List<CcpJsonRepresentation> synonymsOfTheSkill = synonym.getAsJsonList(VisJsonCommonsFields.synonym);
			Stream<CcpJsonRepresentation> synonymsOfTheSkillStream = synonymsOfTheSkill.stream();
			var synonymNamesStream = synonymsOfTheSkillStream.map(x -> x.getAsString(VisJsonCommonsFields.skill));
			List<String> otherNames = synonymNamesStream.collect(Collectors.toList());
			allNames.add(mainName);
			allNames.addAll(otherNames);
			for (var name : allNames) {
				wordsAndParents.put(name, allParents);
			}
			String mainSkillName = synonym.getAsString(VisJsonCommonsFields.skill);

			String skill = mainSkillName.toUpperCase();
			wordsAndSkills.put(skill, skill);
			{
				List<CcpJsonRepresentation> words = synonym.getAsJsonList(VisJsonCommonsFields.synonym);
				for (CcpJsonRepresentation word : words) {
					String synonymName = word.getAsString(VisJsonCommonsFields.skill);
					String upperCase = synonymName.toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getAsJsonList(JsonFields.preRequisite);
				for (CcpJsonRepresentation word : words) {
					String preRequisiteWord = word.getAsString(VisJsonCommonsFields.word);
					String upperCase = preRequisiteWord.toUpperCase();
					wordsAndSkills.put(upperCase, skill);
				}
			}
			{
				List<CcpJsonRepresentation> words = synonym.getAsJsonList(JsonFields.similar);
				for (CcpJsonRepresentation word : words) {
					String similarWord = word.getAsString(VisJsonCommonsFields.word);
					String upperCaseSimilarWord = similarWord.toUpperCase();
					String upperCase = upperCaseSimilarWord.replace("_", " ");
					wordsAndSkills.put(upperCase, skill);
				}
			}
		}
		CcpJsonRepresentation groupedSkills = CcpOtherConstants.EMPTY_JSON;
		Set<String> words = wordsAndSkills.keySet();
		
		for (String word : words) {
			int wordLength = word.length();
			boolean wordIsTooShort = wordLength < 2;
			if(wordIsTooShort) {
				continue;
			}
			int sameWordLength = word.length();
			boolean wordIsTooLong = sameWordLength > 50;

			if(wordIsTooLong) {
				continue;
			}
			
			String initials = word.substring(0, 2);
			String skill = wordsAndSkills.get(word);
			CcpFieldName initialsFieldName = new CcpFieldName(initials);
			List<CcpJsonRepresentation> skillsWithTheseInitials = groupedSkills.getAsJsonList(initialsFieldName);

			ArrayList<CcpJsonRepresentation> updatedSkillsWithTheseInitials = new ArrayList<>(skillsWithTheseInitials);
			Set<String> parent = wordsAndParents.getOrDefault(word, new HashSet<>());
			CcpJsonRepresentation jsonWithSkill = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.skill, skill);
					CcpJsonRepresentation jsonWithSkillAndWord = jsonWithSkill
					.put(VisJsonCommonsFields.word, word);
					CcpJsonRepresentation json = jsonWithSkillAndWord
					.put(VisJsonCommonsFields.parent, parent)
					;
			updatedSkillsWithTheseInitials.add(json);
			CcpFieldName sameInitialsFieldName = new CcpFieldName(initials);

			groupedSkills = groupedSkills.put(sameInitialsFieldName, updatedSkillsWithTheseInitials);
		}
		CcpJsonRepresentation groupedSkillsSnapshot = new CcpJsonRepresentation(groupedSkills.content);
		Set<String> fieldSet = groupedSkills.fieldSet();
		Stream<String> initialsStream = fieldSet.stream();
		var groupedSkillsStream = initialsStream
		.map(initials -> {
			CcpFieldName initialsKey = new CcpFieldName(initials);
			List<CcpJsonRepresentation> skill = groupedSkillsSnapshot.getAsJsonList(initialsKey);
			CcpJsonRepresentation jsonWithSkills = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.skill, skill);
					CcpJsonRepresentation json = jsonWithSkills
					.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, initials)
					;
			return json
		;
		});
		var bulkItemsStream = groupedSkillsStream
		.map(json -> new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY, ENTITY.calculateId(json)));
		List<CcpBulkItem> bulkItems = bulkItemsStream
		.collect(Collectors.toList());
		
		
		return bulkItems;
	}	
	
	/** Fields of the synonym file. */
	static enum JsonFields implements CcpJsonFieldName{
		/** The {@code similar} field. */
		similar,
		/** The {@code preRequisite} field. */
		preRequisite}
}
