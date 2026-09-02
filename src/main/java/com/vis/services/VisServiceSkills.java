package com.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntityOperationType;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityGroupPositionsBySkills;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.ccp.especifications.db.crud.CcpSelectProcedure;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import java.util.stream.Stream;

enum Fields implements CcpJsonFieldName{
	text,
	excludedSkill,
	label, 
	discardedSkills,
	isPieceOfOtherWord,
	associated,
	isPieceOfOtherSkill, 
	skillAlreadyAdded 
}

/**
 * Serviço de operações sobre skills: solicitação de novas skills, extração de skills de texto livre
 * e correção de hierarquia. Contém a lógica mais rica do módulo de skills.
 */
public enum VisServiceSkills implements JnService {
	
	RequestToCreateNewSkill{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpEntityMetaData entityMetaData = VisEntitySkillPending.ENTITY.getEntityMetaData();
			CcpBusiness action = entityMetaData.getOperationCallback(CcpEntityOperationType.save);
			CcpGetEntityId ccpGetEntityId = new CcpGetEntityId(json);
			CcpSelectProcedure toBeginProcedureAnd = ccpGetEntityId
			.toBeginProcedureAnd();
			var ifThisIdIsPresentInEntity = toBeginProcedureAnd
			.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY);
			var returnStatus = ifThisIdIsPresentInEntity.returnStatus(RequestToCreateNewSkillStatus.rejected);
			var and = returnStatus
			.and();
			var ifThisIdIsPresentInEntity2 = and
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY);
			var returnStatus2 = ifThisIdIsPresentInEntity2.returnStatus(RequestToCreateNewSkillStatus.pending);
			var and2 = returnStatus2
			.and();
			CcpEntity twinEntity = VisEntitySkillPending.ENTITY.getTwinEntity();
			var ifThisIdIsPresentInEntity3 = and2
			.ifThisIdIsPresentInEntity(twinEntity);
			var returnStatus3 = ifThisIdIsPresentInEntity3.returnStatus(RequestToCreateNewSkillStatus.approved);
			var and3 = returnStatus3
			.and();
			var ifThisIdIsNotPresentInEntity = and3
			.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY);
			var executeAction = ifThisIdIsNotPresentInEntity.executeAction(action);
			var and4 = executeAction
			.and();
			var ifThisIdIsPresentInEntity4 = and4
			.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY);
			var returnStatus4 = ifThisIdIsPresentInEntity4.returnStatus(RequestToCreateNewSkillStatus.alreadyAdded);
			var andFinallyReturningTheseFields = returnStatus4
			.andFinallyReturningTheseFields();
			andFinallyReturningTheseFields
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			
			CcpJsonRepresentation throwException = RequestToCreateNewSkillStatus.analyzing.throwException(json);
			
			return throwException;
		}
		
	},
	
	GetSkillsFromText{

		private boolean isAlreadyInCache(CcpJsonRepresentation json) {
			String id = VisEntityGroupPositionsBySkills.ENTITY.calculateId(json);
			CcpCacheDecorator cache = new CcpCacheDecorator(id);
			boolean presentInTheCache = cache.isPresentInTheCache();
			return presentInTheCache;
		}
		
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String asString = json.getAsString(Fields.text);
			String text = asString.toUpperCase();
			String textTrim = text.trim();
		
			boolean emptyText = textTrim.isEmpty();
			
			if(emptyText) {
				return CcpOtherConstants.EMPTY_JSON;
			}
			
			String[] phrases = text.split(CcpOtherConstants.DELIMITERS);

			Set<CcpJsonRepresentation> idsToSearch = new HashSet<>();
			Map<String, CcpJsonRepresentation> allWordsGroups = new HashMap<>();
		
			for (String phrase : phrases) {
				int phraseLength = phrase.length();
			
				boolean tooSmallPhrase = phraseLength < 2;
				
				if(tooSmallPhrase) {
					continue;
				}
				
				String firstTwoInitials = phrase.substring(0, 2);
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, firstTwoInitials);
				String id = VisEntityGroupPositionsBySkills.ENTITY.calculateId(put);
				allWordsGroups.put(id, put);
				
				boolean alreadyInCache = this.isAlreadyInCache(put);
				if(alreadyInCache) {
					continue;
				}
				
				idsToSearch.add(put);
			}
			
			CcpEntityMetaData entityDetails = VisEntityGroupPositionsBySkills.ENTITY.getEntityMetaData();

			CcpJsonRepresentation multipleByIds = entityDetails.getMultipleByIds(idsToSearch);
			
			List<CcpJsonRepresentation> allSkillsFoundInTheText = new ArrayList<>(); 
			 
			Set<String> ids = allWordsGroups.keySet();
			
			for (String id : ids) {
				CcpCacheDecorator cache = new CcpCacheDecorator(id);
				
				CcpJsonRepresentation innerJson = cache.get(jsn -> multipleByIds.getInnerJson(new CcpFieldName(id)), 3600);
				
				boolean idNotFound = innerJson.isEmpty();
				
				if(idNotFound) {
					continue;
				}
				
				List<CcpJsonRepresentation> skills = innerJson.getAsJsonList(VisJsonCommonsFields.skill);
				
				for (CcpJsonRepresentation skill : skills) {
					String asString2 = skill.getAsString(VisJsonCommonsFields.word);
					String word = asString2.toUpperCase();
					boolean found = text.contains(word);
					if(found) {
						allSkillsFoundInTheText.add(skill);
						continue;
					}
				}
			}
			
			CcpJsonRepresentation discardedSkills = CcpOtherConstants.EMPTY_JSON;
			List<CcpJsonRepresentation> excludedSkill = json.getAsJsonList(com.vis.services.GetSkillsFromText.excludedSkill);
			Stream<CcpJsonRepresentation> stream = excludedSkill.stream();
			var streamMap = stream.map(x -> x.getAsString(VisJsonCommonsFields.word).toUpperCase());

			List<String> excluded = streamMap.collect(Collectors.toList());
			
			List<CcpJsonRepresentation> choosedSkills = new ArrayList<>();
			Stream<String> stream2 = Arrays.asList(phrases).stream();
			var stream2Map = stream2.map(phrase -> phrase.replaceAll(CcpOtherConstants.DELIMITERS, ""));

			List<String> phrasesList = stream2Map.collect(Collectors.toList());
			
			for (CcpJsonRepresentation skill : allSkillsFoundInTheText) {
				String asString3 = skill.getAsString(VisJsonCommonsFields.word);
			
				String word = asString3.toUpperCase();

				boolean excludedWord = excluded.contains(word);
				
				if(excludedWord) {
					continue;
				}
				int wordLength = word.length();

				boolean isTooSmallWord = wordLength < 7;
			
				CcpJsonRepresentation jsonPiece = skill.getJsonPiece(VisJsonCommonsFields.skill, VisJsonCommonsFields.word);
				
				if(isTooSmallWord) {
					String replaceAll = word.replaceAll(CcpOtherConstants.DELIMITERS, "");
					boolean contains = phrasesList.contains(replaceAll);
					boolean isNotAnIndepententWord = false == contains;
					if(isNotAnIndepententWord) {
						Stream<String> stream3 = phrasesList.stream();
						var filter = stream3.filter(phrase -> phrase.toUpperCase().contains(replaceAll.toUpperCase()));
						Optional<String> findFirst = filter.findFirst();
						boolean findFirstPresent = findFirst.isPresent();
						boolean valorIgual = false == findFirstPresent;
					
						if(valorIgual) {
							continue;
						}
						String associated = findFirst.get();
						CcpJsonRepresentation put = jsonPiece.put(Fields.associated, associated);
						discardedSkills = discardedSkills.addToList(Fields.isPieceOfOtherWord, put)
								;
						continue;
					}
					
					CcpJsonRepresentation putLabel = this.putLabel(skill);
					choosedSkills.add(putLabel);
					continue;
				}
				Stream<CcpJsonRepresentation> stream4 = allSkillsFoundInTheText.stream();
				var filter2 = stream4.filter(x -> x.getAsString(VisJsonCommonsFields.word).length() > word.length());
				var filter3 = filter2.filter(x -> x.getAsString(VisJsonCommonsFields.word).contains(word));

				Optional<CcpJsonRepresentation> findFirst = filter3.findFirst();
				boolean isPieceOfOtherSkill = findFirst.isPresent();
				if(isPieceOfOtherSkill) {
					CcpJsonRepresentation jsn = findFirst.get();
					String associated = jsn.getAsString(VisJsonCommonsFields.word);
					CcpJsonRepresentation put = jsonPiece.put(Fields.associated, associated);
					discardedSkills = discardedSkills.addToList(Fields.isPieceOfOtherSkill, put);
					continue;
				}
				CcpJsonRepresentation putLabel = this.putLabel(skill);
				choosedSkills.add(putLabel);
			}
			
			choosedSkills.sort((a, b) -> a.getAsString(Fields.label).length() -  b.getAsString(Fields.label).length());
			
			Map<String, CcpJsonRepresentation> map = new LinkedHashMap<>();
		
			for (CcpJsonRepresentation skill : choosedSkills) {
				String skillName = skill.getAsString(VisJsonCommonsFields.skill);
				boolean alreadyAdded = map.containsKey(skillName);
				
				if(alreadyAdded){
					CcpJsonRepresentation jsn = map.get(skillName);
					String associated = jsn.getAsString(VisJsonCommonsFields.word);
					CcpJsonRepresentation jsonPiece = skill.getJsonPiece(VisJsonCommonsFields.skill, VisJsonCommonsFields.word);
					CcpJsonRepresentation put = jsonPiece.put(Fields.associated, associated);
					discardedSkills = discardedSkills.addToList(Fields.skillAlreadyAdded, put);
					continue;
				}
				List<String> asStringList = skill.getAsStringList(VisJsonCommonsFields.parent);
				Stream<String> stream5 = asStringList
						.stream();
						var stream5Map = stream5
						.map(x -> x.endsWith("123") ? x.substring(0, x.length() - 3) : x);

						List<String> parent = stream5Map
						.collect(Collectors.toList());
				
				CcpJsonRepresentation put = skill.put(VisJsonCommonsFields.parent, parent);
				
				map.put(skillName, put);
			}
			
			Collection<CcpJsonRepresentation> skills = map.values();
			CcpJsonRepresentation put2 = CcpOtherConstants.EMPTY_JSON
					.put(Fields.discardedSkills, discardedSkills);
					CcpJsonRepresentation put3 = put2
					.put(Fields.excludedSkill, excludedSkill);

					CcpJsonRepresentation put = put3
					.put(VisJsonCommonsFields.skill, skills)
;
			return put;
		}
		
		private CcpJsonRepresentation putLabel(CcpJsonRepresentation json) {
			CcpFieldName ccpFieldName = new CcpFieldName("skill");
			String skill = json.getAsString(ccpFieldName);
			CcpFieldName ccpFieldName2 = new CcpFieldName("word");
			String word = json.getAsString(ccpFieldName2);

			boolean sameWord = skill.equals(word);
			if(sameWord) {
				CcpFieldName ccpFieldName3 = new CcpFieldName("label");
				CcpJsonRepresentation put = json.put(ccpFieldName3, skill);
				return put;
			}
			String wordMais = word + " (";
			String wordMaisMais = wordMais + skill;
			String label = wordMaisMais + ")";
			CcpFieldName ccpFieldName4 = new CcpFieldName("label");
			CcpJsonRepresentation put = json.put(ccpFieldName4, label);
			return put;
		}
	}, 
	
	FixSkillHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation save = VisEntitySkillFixHierarchyPending.ENTITY.save(json);
			return save;
		}
	}
	;
	
	static int getWordStatus(CcpJsonRepresentation group, String word) {
		String initials = word.substring(0,2);
		CcpFieldName ccpFieldName5 = new CcpFieldName(initials);
		boolean containsAllFields = group.containsAllFields(ccpFieldName5);
		boolean notContainsInitials = false == containsAllFields;

		if(notContainsInitials) {
			return 1;
		}
		CcpFieldName ccpFieldName6 = new CcpFieldName(initials);
		Set<String> set = group.getAsObject(ccpFieldName6);
		boolean contains2 = set.contains(word);
		boolean notContains = false == contains2;
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
		@CcpJsonFieldTypeNestedJson(jsonValidation = ExcludedSkillFields.class)
		excludedSkill
	}
	
	
	enum ExcludedSkillFields implements CcpJsonFieldName{
		
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		skill, 

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		word 
	
	}
