package com.vis.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.ccp.decorators.CcpFileDecorator;
import java.util.stream.Stream;

/**
 * Representa uma skill (habilidade) aprovada no sistema, com seu ranking de relevância (baseado na
 * quantidade de currículos que a possuem), suas skills-pai na hierarquia e seus sinônimos.
 * Possui cache de 1 hora. Inclui lógica de carga inicial que lê synonyms.json e um arquivo de
 * contagem de palavras por currículo para calcular o ranking.
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
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\jn\\skills\\synonyms.json");
		CcpFileDecorator ccpStringDecoratorFile = ccpStringDecorator
		.file();
		var asJsonList = ccpStringDecoratorFile
		.asJsonList();
		var stream = asJsonList
		.stream();
		var filter = stream
		.filter(x -> x.getAsString(VisJsonCommonsFields.skill).length() <= 50);
		var synonyms = filter
		.collect(Collectors.toList())
		;
		CcpStringDecorator ccpStringDecorator2 = new CcpStringDecorator("..\\ccp_rest-api-tests_jobsnow\\documentation\\vis\\database\\skills\\countByWords.txt");
		CcpFileDecorator ccpStringDecorator2File = ccpStringDecorator2
				 .file();
				 List<String> lines = ccpStringDecorator2File.getLines()
				 ;
				 var stream2 = synonyms.stream();
				 var stream2Map = stream2.map(json -> {
			int resumesCount = this.getResumesCount(json, lines);
			CcpFieldName ccpFieldName = new CcpFieldName("resumesCount");

			CcpJsonRepresentation put = json.put(ccpFieldName, resumesCount);
			
			return put;
			
			});
			var collect2 = stream2Map.collect(Collectors.toList());

			List<CcpJsonRepresentation> collect = new ArrayList<>(collect2);
		
		
		collect.sort((a, b) -> b.getAsIntegerNumber(new CcpFieldName("resumesCount")) - a.getAsIntegerNumber(new CcpFieldName("resumesCount")));
		
		int ranking = 1;
		
		List<CcpBulkItem> response = new ArrayList<>();
		
		for (CcpJsonRepresentation json : collect) {
			VisEntitySkill.Fields[] fieldsValues = Fields.values();
			CcpJsonRepresentation jsonPiece = json.getJsonPiece(fieldsValues);
			boolean rankingIgual = ranking == 87;
			if(rankingIgual) {
				System.out.println();
			}
			CcpJsonRepresentation put = jsonPiece.put(VisJsonCommonsFields.ranking, ranking++);
			List<CcpJsonRepresentation> asJsonList2 = put.getAsJsonList(VisJsonCommonsFields.synonym);
			Stream<CcpJsonRepresentation> stream3 = asJsonList2.stream();
			var stream3Map = stream3
					.map(x -> x.getAsString(VisJsonCommonsFields.skill));
					var filter2 = stream3Map
					.filter(x -> x.length() <= 50);
					var synonym = filter2
					.collect(Collectors.toList());
			
			
			put = put.put(VisJsonCommonsFields.synonym, synonym);
			
			var items = ENTITY.toBulkItems(put, CcpBulkEntityOperationType.create);
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
			 
			 String start = word + " = ";
			 var stream4 = new ArrayList<>(lines).stream();
			 var filter3 = stream4.filter(line -> line.startsWith(start));
			 var filter3Map = filter3.map(line -> line.replace(start, "").trim());
			 var filter3MapMap = filter3Map
			 .map(line -> Integer.valueOf(line));
			 var findFirst = filter3MapMap
			 .findFirst();

			 Integer orElse = findFirst
			 .orElse(0);
			 
			 total += orElse;
		}
		 
		return total;
	}
	
}

