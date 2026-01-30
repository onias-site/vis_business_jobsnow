package com.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpDynamicJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.vis.entities.VisEntityGroupPositionsBySkills;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsSkillsGroupedByResumes;

enum Fields implements CcpJsonFieldName{
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString(maxLength = 5_000_000, allowsEmptyString = true)
	text,
	@CcpJsonFieldValidatorArray
	@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSkillsGroupedByResumes.class)
	excludedSkill,
	word,
	skill,
	label,
	parent, 
	discardedSkills,
	isPieceOfOtherWord, 
	isPieceOfOtherSkill, 
	skillAlreadyAdded, 
}

public enum VisServiceSkills implements JnService {
	GetSkillsFromText{

		private boolean isInCache(CcpJsonRepresentation json) {
			String id = VisEntityGroupPositionsBySkills.ENTITY.calculateId(json);
			CcpCacheDecorator cache = new CcpCacheDecorator(id);
			boolean presentInTheCache = cache.isPresentInTheCache();
			return presentInTheCache;
		}
		
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

			String text = json.getAsString(Fields.text).toUpperCase();
		
			if(text.trim().isEmpty()) {
				return CcpOtherConstants.EMPTY_JSON;
			}
			
			String[] phrases = text.split(CcpOtherConstants.DELIMITERS);

			Set<CcpJsonRepresentation> idsToSearch = new HashSet<>();
			Map<String, CcpJsonRepresentation> allWordsGroups = new HashMap<>();
		
			for (String phrase : phrases) {
				if(phrase.length() < 2) {
					continue;
				}
				String firstTwoInitials = phrase.substring(0, 2);
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, firstTwoInitials);
				String id = VisEntityGroupPositionsBySkills.ENTITY.calculateId(put);
				allWordsGroups.put(id, put);
				
				if(this.isInCache(put)) {
					continue;
				}
				
				idsToSearch.add(put);
			}
			
			CcpJsonRepresentation multipleByIds = VisEntityGroupPositionsBySkills.ENTITY.getMultipleByIds(idsToSearch);
			
			
			List<CcpJsonRepresentation> allSkillsFoundInTheText = new ArrayList<>();
			
			Set<String> ids = allWordsGroups.keySet();
			
			for (String id : ids) {
				CcpCacheDecorator cache = new CcpCacheDecorator(id);
				
				CcpJsonRepresentation innerJson = cache.get(jsn -> multipleByIds.getDynamicVersion().getInnerJson(id), 3600);
				
				boolean idNotFound = innerJson.isEmpty();
				
				if(idNotFound) {
					continue;
				}
				
				List<CcpJsonRepresentation> skills = innerJson.getAsJsonList(VisEntityGroupPositionsBySkills.Fields.skill);
				
				for (CcpJsonRepresentation skill : skills) {
					String word = skill.getAsString(Fields.word).toUpperCase();
					boolean found = text.contains(word);
					if(found) {
						allSkillsFoundInTheText.add(skill);
						continue;
					}
				}
			}
			
			CcpJsonRepresentation discardedSkills = CcpOtherConstants.EMPTY_JSON;
			List<CcpJsonRepresentation> excludedSkill = json.getAsJsonList(VisEntityResume.Fields.excludedSkill);
			
			List<String> excluded = excludedSkill.stream().map(x -> x.getAsString(Fields.word).toUpperCase()).collect(Collectors.toList());
			
			List<CcpJsonRepresentation> choosedSkills = new ArrayList<>();
		
			List<String> phrasesList = Arrays.asList(phrases).stream().map(phrase -> phrase.replaceAll(CcpOtherConstants.DELIMITERS, "")).collect(Collectors.toList());
			
			for (CcpJsonRepresentation skill : allSkillsFoundInTheText) {
				
				String word = skill.getAsString(Fields.word).toUpperCase();

				boolean excludedWord = excluded.contains(word);
				
				if(excludedWord) {
					continue;
				}
				
				
				boolean isTooSmallWord = word.length() < 7;
				if(isTooSmallWord) {
					String replaceAll = word.replaceAll(CcpOtherConstants.DELIMITERS, "");
					if(false == phrasesList.contains(replaceAll)) {
						discardedSkills = discardedSkills
								.addToList(Fields.isPieceOfOtherWord, skill.getJsonPiece(Fields.skill, Fields.word))
								;
						continue;
					}
					
					CcpJsonRepresentation putLabel = this.putLabel(skill);
					choosedSkills.add(putLabel);
					continue;
				}
				
				boolean isPieceOfOtherSkill = allSkillsFoundInTheText.stream().filter(x -> x.getAsString(Fields.word).length() > word.length()).anyMatch(x -> x.getAsString(Fields.word).contains(word));
				if(isPieceOfOtherSkill) {
					discardedSkills = discardedSkills
							.addToList(Fields.isPieceOfOtherSkill, skill.getJsonPiece(Fields.skill, Fields.word))
							;
					continue;
				}
				CcpJsonRepresentation putLabel = this.putLabel(skill);
				choosedSkills.add(putLabel);
			}
			
			choosedSkills.sort((a, b) -> a.getAsString(Fields.label).length() -  b.getAsString(Fields.label).length());
			
			Map<String, CcpJsonRepresentation> map = new LinkedHashMap<>();
		
			for (CcpJsonRepresentation skill : choosedSkills) {
				String skillName = skill.getAsString(Fields.skill);
				if(map.containsKey(skillName)){
					discardedSkills = discardedSkills
							.addToList(Fields.skillAlreadyAdded, skill.getJsonPiece(Fields.skill, Fields.word));
					continue;

				}
				map.put(skillName, skill);
			}
			Collection<CcpJsonRepresentation> skills = map.values();
			
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
					.put(Fields.discardedSkills, discardedSkills)
					.put(Fields.excludedSkill, excludedSkill)
					.put(Fields.skill, skills)
;
			return put;
		}
		
		private CcpJsonRepresentation putLabel(CcpJsonRepresentation json) {
			CcpDynamicJsonRepresentation dynamicVersion = json.getDynamicVersion();
			String skill = dynamicVersion.getAsString("skill");
			String word = dynamicVersion.getAsString("word");
			
			boolean sameWord = skill.equals(word);
			if(sameWord) {
				CcpJsonRepresentation put = dynamicVersion.put("label", skill);
				return put;
			}
			String label = word + " (" + skill + ")";
			CcpJsonRepresentation put = dynamicVersion.put("label", label);
			return put;
		}
	},
	;
	
	static int getWordStatus(CcpJsonRepresentation group, String word) {
		String initials = word.substring(0,2);
		CcpDynamicJsonRepresentation dynamicVersion = group.getDynamicVersion();
		boolean notContainsInitials = false == dynamicVersion.containsAllFields(initials);
		
		if(notContainsInitials) {
			return 1;
		}
		Set<String> set =  dynamicVersion.getAsObject(initials);
		boolean notContains = false == set.contains(word);
		if(notContains) {
			return 2;
		}
		
		return 0;
	}

}

	enum GetSkillsFromText implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(maxLength = 5_000_000, allowsEmptyString = true)
		text,
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSkillsGroupedByResumes.class)
		excludedSkill,
		implicitSkills
	}
