package com.vis.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.vis.entities.VisEntityGroupPositionsBySkills;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsSkillsGroupedByResumes;

public enum VisServiceSkills implements JnService {
	GetSkillsFromText{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String text = json.getAsString(com.vis.services.GetSkillsFromText.text).toUpperCase();
			if(text.trim().isEmpty()) {
				return CcpOtherConstants.EMPTY_JSON;
			}
			String[] phrases = text.split(CcpOtherConstants.DELIMITERS);

			CcpJsonRepresentation group = CcpOtherConstants.EMPTY_JSON;
			
			List<CcpJsonRepresentation> excludedSkill = json.getAsJsonList(VisEntityResume.Fields.excludedSkill);
			
			List<String> excluded = excludedSkill.stream().map(x -> x.getDynamicVersion().getAsString("word").toUpperCase()).collect(Collectors.toList());
			
			Map<String, CcpJsonRepresentation> response = new HashMap<>();
			

			for (String phrase : phrases) {
				phrase = phrase.trim();
				if(phrase.length() < 2) {
					continue;
				}
				if(phrase.length() > 35) {
					continue;
				}
				
				String firstTwoInitials = phrase.substring(0, 2);
				HashSet<String> orDefault = group.getDynamicVersion().getOrDefault(firstTwoInitials, new HashSet<String>());
				orDefault.add(phrase);
				group = group.getDynamicVersion().put(firstTwoInitials, orDefault);
			}
		
			List<CcpJsonRepresentation> collect = group.fieldSet().stream().map(firstTwoInitials -> CcpOtherConstants.EMPTY_JSON.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, firstTwoInitials))
			.collect(Collectors.toList());
			
			CcpJsonRepresentation multipleByIds = VisEntityGroupPositionsBySkills.ENTITY.getMultipleByIds(collect);
			
			Set<String> ids = multipleByIds.fieldSet();
			
			for (String id : ids) {
				CcpJsonRepresentation innerJson = multipleByIds.getDynamicVersion().getInnerJson(id);
			
				if(innerJson.isEmpty()) {
					continue;
				}
				
				List<CcpJsonRepresentation> skills = innerJson.getAsJsonList(VisEntityGroupPositionsBySkills.Fields.skill);
				
				String firstTwoInitials = innerJson.getAsString(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials);
				
				Set<String> phrasesInResume = group.getDynamicVersion().getAsObject(firstTwoInitials);
				
				List<CcpJsonRepresentation> matchedPhrases = skills.stream()
						.filter(skill -> false == this.getPhraseInResume(phrasesInResume, skill, text).isEmpty())
						.filter(skill -> false == excluded.contains(skill.getDynamicVersion().getAsString("word").toUpperCase()))
						.collect(Collectors.toList()); 
				
				
				for (CcpJsonRepresentation matchedPhrase : matchedPhrases) {
					String skill = matchedPhrase.getDynamicVersion().getAsString("skill");
					response.put(skill, matchedPhrase);
				}
			}
			
			List<CcpJsonRepresentation> values = new ArrayList<>(response.values());
			
			values.sort((a, b) -> a.getDynamicVersion().getAsString("word").length() -  b.getDynamicVersion().getAsString("word").length());
			
			Collection<CcpJsonRepresentation> skills = values.stream().filter(skill -> this.isElegibleSkill(values, skill)).collect(Collectors.toList()); 
			
			var implicitSkills = this.getImplicitSkills(skills, excludedSkill);
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
					.put(com.vis.services.GetSkillsFromText.implicitSkills, implicitSkills)
					.put(VisEntityGroupPositionsBySkills.Fields.skill, skills)
					;
			
			return  put;
		}
		
		private List<CcpJsonRepresentation> getImplicitSkills(Collection<CcpJsonRepresentation> skills, List<CcpJsonRepresentation> excludedSkill) {

			Set<String> set = new HashSet<>();
			
			for (CcpJsonRepresentation skill : skills) {
				List<String> parent = skill.getDynamicVersion().getAsStringList("parent");
				set.addAll(parent);
			}
			
			var implicitSkills = set.stream().filter(parent -> new ArrayList<>(skills).stream().map(x -> x.getDynamicVersion())
					.allMatch(skill -> false == skill.getAsString("skill").equals(parent)))
					.filter(parent -> false == excludedSkill.stream().map(skill -> skill.getDynamicVersion()
							.getAsString("skill")).collect(Collectors.toSet()).contains(parent)).collect(Collectors.toSet())
					;
			
			List<CcpJsonRepresentation> response = new ArrayList<>();
			for (String implicitSkill : implicitSkills) {
				CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON;
				List<CcpJsonRepresentation> collect = skills.stream().map(x -> x.getDynamicVersion())
				.filter(x -> x.getAsStringList("parent").contains(implicitSkill))
				.map(x -> x.getJsonPiece("skill", "word")).collect(Collectors.toList())
				;
				CcpJsonRepresentation put = json.getDynamicVersion().put("skill", implicitSkill)
				.getDynamicVersion().put("children", collect);
				
				response.add(put);
			}
			return response;
		}
		
		
		private boolean isPieceFromAnotherWord(Collection<CcpJsonRepresentation> skills, CcpJsonRepresentation skill) {
			String 	word = skill.getDynamicVersion().getAsString("word");
			
			List<CcpJsonRepresentation> collect = skills.stream().filter(x -> x.getDynamicVersion().getAsString("word").length() > word.length()).collect(Collectors.toList());
			
			for (CcpJsonRepresentation sk : collect) {
				boolean contains = sk.getDynamicVersion().getAsString("word").contains(word);
				if(contains) {
					return true;
				}
			}
			
			return false;
		}
		
		private boolean isElegibleSkill(Collection<CcpJsonRepresentation> skills, CcpJsonRepresentation skill) {
			String word = skill.getDynamicVersion().getAsString("word");

			if(word.length() > 4 && false == this.isPieceFromAnotherWord(skills, skill)) {
				return true;
			}
			
			String skillName = skill.getDynamicVersion().getAsString("skill");
			
			boolean isSomeoneParent = skills.stream().map(x -> x.getDynamicVersion().getAsStringList("parent"))
					.anyMatch(x -> x.contains(skillName) || x.contains(word));
			
			if(isSomeoneParent) {
				return true;
			}

			List<String> parent = skill.getDynamicVersion().getAsStringList("parent");
			
			for (CcpJsonRepresentation sk : skills) {
				{
					String asString = sk.getDynamicVersion().getAsString("skill");
					boolean matches = parent.contains(asString);
					if(matches) {
						return true;
					}
				}
				{
					String asString = sk.getDynamicVersion().getAsString("word");
					boolean matches = parent.contains(asString);
					if(matches) {
						return true;
					}
				}
			}
			
			
			return false;
		}
		
		private String sanitizeWord(String upperCase) {
			if(upperCase.endsWith(CcpOtherConstants.DELIMITERS)) {
				upperCase = upperCase.substring(0, upperCase.length() - 1);
				return this.sanitizeWord(upperCase);
			}
			if(upperCase.endsWith("\\.")) {
				upperCase = upperCase.substring(0, upperCase.length() - 1);
				return this.sanitizeWord(upperCase);
			}
			return upperCase;
		}
		
		private String getPhraseInResume(Set<String> phrasesInResume, CcpJsonRepresentation skill, String text) {
			
			for (String phraseInResume : phrasesInResume) {
				{
					
					String upperCase = skill.getDynamicVersion().getAsString("word").toUpperCase();
					upperCase = this.sanitizeWord(upperCase);
					boolean contains = phraseInResume.equals(upperCase);
					if(contains) {
						return phraseInResume;
					}
					
					int length = upperCase.split(CcpOtherConstants.DELIMITERS).length;
					
					if(length <= 1) {
						continue;
					}
					
					boolean containsAll = this.isWithinTheText(text, upperCase);	
					if(containsAll) {
						return upperCase;
					}
				}
			}
			return "";
		}

		private boolean isWithinTheText(String text, String upperCase) {

			int indexOf = text.indexOf(upperCase);
			if(indexOf < 0) {
				return false;
			}
			
			
			char charAt = text.charAt(indexOf + 1);
			
			char[] charArray = CcpOtherConstants.DELIMITERS.toCharArray();
			for (char c : charArray) {
				if(c == charAt) {
					return true;
				}
			}
			
			return false;
		}
	},
	;
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
