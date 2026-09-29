package com.vis.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.ccp.decorators.CcpFileDecorator;
import java.util.stream.Stream;

/**
 * Represents a skill approved in the system, with its relevance ranking (based on the number of
 * resumes that have it), its parent skills in the hierarchy and its synonyms.
 * Cached for 1 hour. Includes the initial load logic, which reads synonyms.json and a file with
 * the word count per resume to calculate the ranking.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkill.Fields.class)
public class VisEntitySkill implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkill.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		parent,
		
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking,

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill, 
		
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,
		;
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpStringDecorator synonymsFilePath = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json");
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
		CcpStringDecorator countByWordsFilePath = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\vis\\database\\skills\\countByWords.txt");
		CcpFileDecorator countByWordsFile = countByWordsFilePath
				 .file();
				 List<String> lines = countByWordsFile.getLines()
				 ;
				 var synonymsStream = synonyms.stream();
				 var synonymsWithResumesCountStream = synonymsStream.map(json -> {
			int resumesCount = this.getResumesCount(json, lines);

			CcpJsonRepresentation jsonWithResumesCount = json.put(VisJsonCommonsFields.resumesCount, resumesCount);
			
			return jsonWithResumesCount;
			
			});
			var synonymsWithResumesCount = synonymsWithResumesCountStream.collect(Collectors.toList());

			List<CcpJsonRepresentation> sortedSynonyms = new ArrayList<>(synonymsWithResumesCount);
		
		
		sortedSynonyms.sort((a, b) -> b.getAsIntegerNumber(VisJsonCommonsFields.resumesCount) - a.getAsIntegerNumber(VisJsonCommonsFields.resumesCount));
		
		int ranking = 1;
		
		List<CcpBulkItem> response = new ArrayList<>();
		
		for (CcpJsonRepresentation json : sortedSynonyms) {
			VisEntitySkill.Fields[] fieldsValues = Fields.values();
			CcpJsonRepresentation jsonPiece = json.getJsonPiece(fieldsValues);
			boolean isDebugRanking = ranking == 87;
			if(isDebugRanking) {
				System.out.println();
			}
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
			
			var items = ENTITY.toBulkItems(skillRecord, CcpBulkEntityOperationType.create);
			response.addAll(items);
		}
		
		return response;
	}
	
	private int getResumesCount(CcpJsonRepresentation json, List<String> lines) {
		String skill = json.getAsString(VisJsonCommonsFields.skill);
		List<String> synonym = json.getAsStringList(VisJsonCommonsFields.synonym);
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

