package com.vis.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpStoredParentIdsField;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnEntityGrouperParents;
import com.jn.entities.decorators.engine.JnGrouperChildEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnGroupers;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisSkillInitialsCombinations;
import com.ccp.decorators.CcpFileDecorator;
import java.util.stream.Stream;

/**
 * Represents a skill approved in the system, with its relevance ranking (based on the number of
 * resumes that have it), its parent skills in the hierarchy and its synonyms.
 * Cached for 1 hour. Includes the initial load logic, which reads synonyms.json and a file with
 * the word count per resume to calculate the ranking.
 *
 * <p>Each skill is a child, in {@code JnEntityGrouper}, of the group of the first two letters of each of its words (the
 * skill itself, each synonym and each parent, {@link VisEntityVirtualSkillInitialsGrouper}): the lookup the reading of
 * skills from a text uses. The relationship is manyToMany, since the groups change with the words of the skill, and
 * {@code initialsGroup} keeps the groups the skill was written in.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntitySkill.Fields.class)
@CcpEntityGrouperChild(value = {VisEntityVirtualSkillInitialsGrouper.class}, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class)
public class VisEntitySkill implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntitySkill INSTANCE = new VisEntitySkill();

	private VisEntitySkill() {}

	/** The entity {@code vis_skill}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkill.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, list. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		parent,
		
		/** The {@code ranking} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking,

		/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, part of the primary key. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill, 
		
		/** The {@code synonym} field: validated as in {@code VisJsonCommonsFields}, list. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,

		/**
		 * The {@code initialsGroup} field: list; filled on every save by the decorator of {@code @CcpEntityGrouperChild},
		 * it holds the parent id of each group of {@link VisEntityVirtualSkillInitialsGrouper} the skill is in, one per
		 * combination of {@link VisSkillInitialsCombinations}.
		 */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		@CcpStoredParentIdsField(value = VisSkillInitialsCombinations.class, parent = VisEntityVirtualSkillInitialsGrouper.class)
		initialsGroup,
		;
	}
	
	/**
	 * Seeds the skills from {@code documentation/jn/skills/synonyms.json} (skills and synonyms of at most 50 characters),
	 * ranked by how many resumes mention them, as counted in {@code documentation/vis/database/skills/countByWords.txt}.
	 * The seed is written by bulk, which the decorator of {@code @CcpEntityGrouperChild} does not see, so it also brings
	 * each group of initials whole, with the ids of all its skills, and each skill brings the ids of its groups.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpStringDecorator synonymsFilePath = new CcpStringDecorator("../ccp_rest-api-tests_jobsnow/documentation/jn/skills/synonyms.json");
		CcpFileDecorator synonymsFile = synonymsFilePath
		.file();
		var allSynonyms = synonymsFile
		.asJsonList();
		var allSynonymsStream = allSynonyms
		.stream();
		var synonymsWithValidSkillLength = allSynonymsStream
		.filter(x -> x.getAsString(VisJsonCommonsFields.skill).length() <= 50);
		var synonyms = synonymsWithValidSkillLength
		.collect(Collectors.toList())
		;
		CcpStringDecorator countByWordsFilePath = new CcpStringDecorator("../ccp_rest-api-tests_jobsnow/documentation/vis/database/skills/countByWords.txt");
		CcpFileDecorator countByWordsFile = countByWordsFilePath
				 .file();
				 List<String> lines = countByWordsFile.getLines()
				 ;
				 var synonymsStream = synonyms.stream();
				 var synonymsWithResumesCountStream = synonymsStream.map(new VisSkillWithResumesCount(lines));
			var synonymsWithResumesCount = synonymsWithResumesCountStream.collect(Collectors.toList());

			List<CcpJsonRepresentation> sortedSynonyms = new ArrayList<>(synonymsWithResumesCount);
		
		
		sortedSynonyms.sort((a, b) -> b.getAsIntegerNumber(VisJsonCommonsFields.resumesCount) - a.getAsIntegerNumber(VisJsonCommonsFields.resumesCount));
		
		int ranking = 1;

		List<CcpBulkItem> response = new ArrayList<>();
		JnEntityGrouperParents initialsGroups = new JnEntityGrouperParents(VisEntityVirtualSkillInitialsGrouper.class, VisEntitySkill.class);
		Map<String, CcpJsonRepresentation> parentJsonByParentId = new LinkedHashMap<>();
		Map<String, Set<String>> skillIdsByParentId = new LinkedHashMap<>();

		for (CcpJsonRepresentation json : sortedSynonyms) {
			VisEntitySkill.Fields[] fieldsValues = Fields.values();
			CcpJsonRepresentation jsonPiece = json.getJsonPiece(fieldsValues);
			CcpJsonRepresentation skillRecord = jsonPiece.put(VisJsonCommonsFields.ranking, ranking++);
			List<CcpJsonRepresentation> synonymsOfTheSkill = skillRecord.getAsJsonList(VisJsonCommonsFields.synonym);
			Stream<CcpJsonRepresentation> synonymsOfTheSkillStream = synonymsOfTheSkill.stream();
			var synonymNamesStream = synonymsOfTheSkillStream
					.map(x -> x.getAsString(VisJsonCommonsFields.skill));
					var synonymNamesWithValidLength = synonymNamesStream
					.filter(x -> x.length() <= 50);
					var synonym = synonymNamesWithValidLength
					.collect(Collectors.toList());
			
			
			skillRecord = skillRecord.put(VisJsonCommonsFields.synonym, synonym);

			Map<String, CcpJsonRepresentation> skillParents = initialsGroups.getParents(skillRecord);
			CcpJsonRepresentation skillRecordWithGroups = initialsGroups.putParentIds(skillRecord, skillParents);
			String skillId = ENTITY.calculateId(skillRecord);
			parentJsonByParentId.putAll(skillParents);
			Set<String> parentIds = skillParents.keySet();
			for (String parentId : parentIds) {
				Set<String> skillIds = skillIdsByParentId.computeIfAbsent(parentId, newParentId -> new LinkedHashSet<>());
				skillIds.add(skillId);
			}

			var items = ENTITY.toBulkItems(skillRecordWithGroups, CcpBulkEntityOperationType.create);
			response.addAll(items);
		}

		Map<String, List<String>> childrenByParentId = new LinkedHashMap<>();
		Set<String> groupedParentIds = skillIdsByParentId.keySet();
		for (String parentId : groupedParentIds) {
			Set<String> skillIds = skillIdsByParentId.get(parentId);
			List<String> children = new ArrayList<>(skillIds);
			childrenByParentId.put(parentId, children);
		}
		String parentName = initialsGroups.getParentName();
		CcpEntityMetaData entityDetails = ENTITY.getEntityMetaData();
		List<CcpBulkItem> groupItems = JnGroupers.getItemsToCreateGroups(parentName, entityDetails.entityName, parentJsonByParentId, childrenByParentId);
		response.addAll(groupItems);

		return response;
	}
	
	/**
	 * Sums the resume counts of the skill and of its synonyms (lines {@code word = count} of the count file).
	 * @param json the skill
	 * @param lines the lines of the count file
	 * @return the number of resumes
	 */
	static int getResumesCount(CcpJsonRepresentation json, List<String> lines) {
		String skill = json.getAsString(VisJsonCommonsFields.skill);
		List<CcpJsonRepresentation> synonymRecords = json.getAsJsonList(VisJsonCommonsFields.synonym);
		Stream<CcpJsonRepresentation> synonymRecordsStream = synonymRecords.stream();
		var synonymNamesStream = synonymRecordsStream.map(x -> x.getAsString(VisJsonCommonsFields.skill));
		List<String> synonym = synonymNamesStream.collect(Collectors.toList());
		Set<String> skills = new HashSet<>(synonym);
		skills.add(skill);
		
		 int total = 0;
		 
		 for (String word : skills) {
			 
			 String linePrefix = word + " = ";
			 var linesStream = new ArrayList<>(lines).stream();
			 var linesOfTheWord = linesStream.filter(line -> line.startsWith(linePrefix));
			 var countTextsOfTheWord = linesOfTheWord.map(line -> line.replace(linePrefix, "").trim());
			 var countsOfTheWord = countTextsOfTheWord
			 .map(line -> Integer.valueOf(line));
			 var firstCountOfTheWord = countsOfTheWord
			 .findFirst();

			 Integer wordResumesCount = firstCountOfTheWord
			 .orElse(0);
			 
			 total += wordResumesCount;
		}
		 
		return total;
	}
	
}

