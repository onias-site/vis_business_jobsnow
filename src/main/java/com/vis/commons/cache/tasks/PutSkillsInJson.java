package com.vis.commons.cache.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.entities.VisEntitySkill;

public class PutSkillsInJson implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{

	public final String fieldTextToRead;

	public final String fieldToPut;
	
	public PutSkillsInJson(String fieldTextToRead, String fieldToPut) {
		this.fieldTextToRead = fieldTextToRead;
		this.fieldToPut = fieldToPut;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpCacheDecorator cache = new CcpCacheDecorator("skills");
		List<CcpJsonRepresentation> resultAsList = cache.get(ReadSkillsFromDataBase.INSTANCE, 86400);
		
		CcpTextDecorator text = json.getAsTextDecorator(this.fieldTextToRead).sanitize();
		List<CcpJsonRepresentation> allSkills = new ArrayList<>();
		for (CcpJsonRepresentation skill : resultAsList) {
		
			Set<String> allSynonyms = 
					skill.getAsJsonList(VisEntitySkill.Fields.synonym.name())
					.stream()
					.map(synonym -> synonym.getAsString(VisEntityResume.Fields.skill.name()))
					.collect(Collectors.toSet());
			
			Set<String> wordsThatWasFoundDirectlyInThisText = allSynonyms
			.stream()
			.filter(synonym -> text.contains(synonym))
			.collect(Collectors.toSet());

			boolean thisSkillWasNotFoundInThisText = wordsThatWasFoundDirectlyInThisText.isEmpty();
			
			if(thisSkillWasNotFoundInThisText) {
				continue;
			}
			
			List<String> parents = skill.getAsStringList(VisEntitySkill.Fields.parent.name());
			Set<String> wordsThatWasFoundAsSynonymInThisText = allSynonyms
			.stream()
			.filter(synonym -> wordsThatWasFoundDirectlyInThisText.contains(synonym) == false)
			.collect(Collectors.toSet());
			List<CcpJsonRepresentation> skills = wordsThatWasFoundDirectlyInThisText.stream().map(
					word -> CcpOtherConstants.EMPTY_JSON
										.put(VisEntitySkill.Fields.synonym.name(), wordsThatWasFoundAsSynonymInThisText)
										.put(VisEntitySkill.Fields.parent.name(), parents)
										.put(VisEntitySkill.Fields.skill.name(), word)
					).collect(Collectors.toList());
			allSkills.addAll(skills);
		}
		
		CcpJsonRepresentation jsonWithSkills = json.put(this.fieldToPut, allSkills);
		return jsonWithSkills;
	}

}
