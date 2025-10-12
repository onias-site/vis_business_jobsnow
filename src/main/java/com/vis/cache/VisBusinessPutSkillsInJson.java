package com.vis.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntitySkill;

public class VisBusinessPutSkillsInJson implements CcpBusiness{

	public final CcpJsonFieldName fieldTextToRead;

	public final CcpJsonFieldName fieldToPut;
	
	public VisBusinessPutSkillsInJson(CcpJsonFieldName fieldTextToRead, CcpJsonFieldName fieldToPut) {
		this.fieldTextToRead = fieldTextToRead;
		this.fieldToPut = fieldToPut;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpCacheDecorator cache = new CcpCacheDecorator("skills");
		List<CcpJsonRepresentation> resultAsList = cache.get(VisFunctionReadSkillsFromDataBase.INSTANCE, 86400);
		
		CcpTextDecorator text = json.getDynamicVersion().getAsTextDecorator(this.fieldTextToRead.name()).sanitize();
		List<CcpJsonRepresentation> allSkills = new ArrayList<>();
		for (CcpJsonRepresentation skill : resultAsList) {
		
			Set<String> allSynonyms = 
					skill.getAsJsonList(VisEntitySkill.Fields.synonym)
					.stream()
					.map(synonym -> synonym.getAsString(VisEntityResume.Fields.skill))
					.collect(Collectors.toSet());
			
			Set<String> wordsThatWasFoundDirectlyInThisText = allSynonyms
			.stream()
			.filter(synonym -> text.contains(synonym))
			.collect(Collectors.toSet());

			boolean thisSkillWasNotFoundInThisText = wordsThatWasFoundDirectlyInThisText.isEmpty();
			
			if(thisSkillWasNotFoundInThisText) {
				continue;
			}
			
			List<String> parents = skill.getAsStringList(VisEntitySkill.Fields.parent);
			Set<String> wordsThatWasFoundAsSynonymInThisText = allSynonyms
			.stream()
			.filter(synonym -> false == wordsThatWasFoundDirectlyInThisText.contains(synonym))
			.collect(Collectors.toSet());
			List<CcpJsonRepresentation> skills = wordsThatWasFoundDirectlyInThisText.stream().map(
					word -> CcpOtherConstants.EMPTY_JSON
										.put(VisEntitySkill.Fields.synonym, wordsThatWasFoundAsSynonymInThisText)
										.put(VisEntitySkill.Fields.parent, parents)
										.put(VisEntitySkill.Fields.skill, word)
					).collect(Collectors.toList());
			allSkills.addAll(skills);
		}
		
		CcpJsonRepresentation jsonWithSkills = json.put(this.fieldToPut, allSkills);
		return jsonWithSkills;
	}

}
