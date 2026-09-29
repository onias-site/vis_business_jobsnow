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
 * Service for skill operations: requests for new skills and extraction of skills from free text.
 * Holds the richest logic of the skills module. The hierarchy fix lives in
 * {@link VisServiceSkillFixHierarchy}.
 */
public enum VisServiceSkills implements JnService {
	
	RequestToCreateNewSkill{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpBusiness action = CcpEntityOperationType.save.getOperationCallback(VisEntitySkillPending.ENTITY);
			CcpGetEntityId entityIdGetter = new CcpGetEntityId(json);
			CcpSelectProcedure procedure = entityIdGetter
			.toBeginProcedureAnd();
			var ifPresentInRejectedSkills = procedure
			.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY);
			var statusIfRejected = ifPresentInRejectedSkills.returnStatus(RequestToCreateNewSkillStatus.rejected);
			var afterRejectedCheck = statusIfRejected
			.and();
			var ifPresentInPendingSkills = afterRejectedCheck
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY);
			var statusIfPending = ifPresentInPendingSkills.returnStatus(RequestToCreateNewSkillStatus.pending);
			var afterPendingCheck = statusIfPending
			.and();
			CcpEntity approvedSkillsEntity = VisEntitySkillPending.ENTITY.getTwinEntity();
			var ifPresentInApprovedSkills = afterPendingCheck
			.ifThisIdIsPresentInEntity(approvedSkillsEntity);
			var statusIfApproved = ifPresentInApprovedSkills.returnStatus(RequestToCreateNewSkillStatus.approved);
			var afterApprovedCheck = statusIfApproved
			.and();
			var ifNotPresentInSkills = afterApprovedCheck
			.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY);
			var saveIfNewSkill = ifNotPresentInSkills.executeAction(action);
			var afterSaveAction = saveIfNewSkill
			.and();
			var ifPresentInSkills = afterSaveAction
			.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY);
			var statusIfAlreadyAdded = ifPresentInSkills.returnStatus(RequestToCreateNewSkillStatus.alreadyAdded);
			var andFinallyReturningTheseFields = statusIfAlreadyAdded
			.andFinallyReturningTheseFields();
			andFinallyReturningTheseFields
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			
			CcpJsonRepresentation analyzingResponse = RequestToCreateNewSkillStatus.analyzing.throwException(json);
			
			return analyzingResponse;
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
			String rawText = json.getAsString(Fields.text);
			String text = rawText.toUpperCase();
			String trimmedText = text.trim();
		
			boolean emptyText = trimmedText.isEmpty();
			
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
				CcpJsonRepresentation initialsGroupId = CcpOtherConstants.EMPTY_JSON.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, firstTwoInitials);
				String id = VisEntityGroupPositionsBySkills.ENTITY.calculateId(initialsGroupId);
				allWordsGroups.put(id, initialsGroupId);
				
				boolean alreadyInCache = this.isAlreadyInCache(initialsGroupId);
				if(alreadyInCache) {
					continue;
				}
				
				idsToSearch.add(initialsGroupId);
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
					String foundSkillWord = skill.getAsString(VisJsonCommonsFields.word);
					String word = foundSkillWord.toUpperCase();
					boolean found = text.contains(word);
					if(found) {
						allSkillsFoundInTheText.add(skill);
						continue;
					}
				}
			}
			
			CcpJsonRepresentation discardedSkills = CcpOtherConstants.EMPTY_JSON;
			List<CcpJsonRepresentation> excludedSkill = json.getAsJsonList(com.vis.services.GetSkillsFromText.excludedSkill);
			Stream<CcpJsonRepresentation> excludedSkillStream = excludedSkill.stream();
			var excludedWordsStream = excludedSkillStream.map(x -> x.getAsString(VisJsonCommonsFields.word).toUpperCase());

			List<String> excluded = excludedWordsStream.collect(Collectors.toList());
			
			List<CcpJsonRepresentation> choosedSkills = new ArrayList<>();
			Stream<String> phrasesStream = Arrays.asList(phrases).stream();
			var cleanPhrasesStream = phrasesStream.map(phrase -> phrase.replaceAll(CcpOtherConstants.DELIMITERS, ""));

			List<String> phrasesList = cleanPhrasesStream.collect(Collectors.toList());
			
			for (CcpJsonRepresentation skill : allSkillsFoundInTheText) {
				String candidateSkillWord = skill.getAsString(VisJsonCommonsFields.word);
			
				String word = candidateSkillWord.toUpperCase();

				boolean excludedWord = excluded.contains(word);
				
				if(excludedWord) {
					continue;
				}
				int wordLength = word.length();

				boolean isTooSmallWord = wordLength < 7;
			
				CcpJsonRepresentation jsonPiece = skill.getJsonPiece(VisJsonCommonsFields.skill, VisJsonCommonsFields.word);
				
				if(isTooSmallWord) {
					String cleanWord = word.replaceAll(CcpOtherConstants.DELIMITERS, "");
					boolean isIndependentWord = phrasesList.contains(cleanWord);
					boolean isNotAnIndependentWord = false == isIndependentWord;
					if(isNotAnIndependentWord) {
						Stream<String> phrasesListStream = phrasesList.stream();
						var phrasesContainingWord = phrasesListStream.filter(phrase -> phrase.toUpperCase().contains(cleanWord.toUpperCase()));
						Optional<String> phraseContainingWord = phrasesContainingWord.findFirst();
						boolean phraseFound = phraseContainingWord.isPresent();
						boolean noPhraseFound = false == phraseFound;
					
						if(noPhraseFound) {
							continue;
						}
						String associated = phraseContainingWord.get();
						CcpJsonRepresentation pieceOfOtherWord = jsonPiece.put(Fields.associated, associated);
						discardedSkills = discardedSkills.addToList(Fields.isPieceOfOtherWord, pieceOfOtherWord)
								;
						continue;
					}
					
					CcpJsonRepresentation labeledSkill = this.putLabel(skill);
					choosedSkills.add(labeledSkill);
					continue;
				}
				Stream<CcpJsonRepresentation> skillsFoundStream = allSkillsFoundInTheText.stream();
				var longerSkills = skillsFoundStream.filter(x -> x.getAsString(VisJsonCommonsFields.word).length() > word.length());
				var longerSkillsContainingWord = longerSkills.filter(x -> x.getAsString(VisJsonCommonsFields.word).contains(word));

				Optional<CcpJsonRepresentation> longerSkillContainingWord = longerSkillsContainingWord.findFirst();
				boolean isPieceOfOtherSkill = longerSkillContainingWord.isPresent();
				if(isPieceOfOtherSkill) {
					CcpJsonRepresentation longerSkill = longerSkillContainingWord.get();
					String associated = longerSkill.getAsString(VisJsonCommonsFields.word);
					CcpJsonRepresentation pieceOfOtherSkill = jsonPiece.put(Fields.associated, associated);
					discardedSkills = discardedSkills.addToList(Fields.isPieceOfOtherSkill, pieceOfOtherSkill);
					continue;
				}
				CcpJsonRepresentation labeledSkill = this.putLabel(skill);
				choosedSkills.add(labeledSkill);
			}
			
			choosedSkills.sort((a, b) -> a.getAsString(Fields.label).length() -  b.getAsString(Fields.label).length());
			
			Map<String, CcpJsonRepresentation> chosenSkillsByName = new LinkedHashMap<>();
		
			for (CcpJsonRepresentation skill : choosedSkills) {
				String skillName = skill.getAsString(VisJsonCommonsFields.skill);
				boolean alreadyAdded = chosenSkillsByName.containsKey(skillName);
				
				if(alreadyAdded){
					CcpJsonRepresentation alreadyAddedSkill = chosenSkillsByName.get(skillName);
					String associated = alreadyAddedSkill.getAsString(VisJsonCommonsFields.word);
					CcpJsonRepresentation jsonPiece = skill.getJsonPiece(VisJsonCommonsFields.skill, VisJsonCommonsFields.word);
					CcpJsonRepresentation repeatedSkill = jsonPiece.put(Fields.associated, associated);
					discardedSkills = discardedSkills.addToList(Fields.skillAlreadyAdded, repeatedSkill);
					continue;
				}
				List<String> parentSkills = skill.getAsStringList(VisJsonCommonsFields.parent);
				Stream<String> parentSkillsStream = parentSkills
						.stream();
						var cleanParentSkillsStream = parentSkillsStream
						.map(x -> x.endsWith("123") ? x.substring(0, x.length() - 3) : x);

						List<String> parent = cleanParentSkillsStream
						.collect(Collectors.toList());
				
				CcpJsonRepresentation skillWithCleanParents = skill.put(VisJsonCommonsFields.parent, parent);
				
				chosenSkillsByName.put(skillName, skillWithCleanParents);
			}
			
			Collection<CcpJsonRepresentation> skills = chosenSkillsByName.values();
			CcpJsonRepresentation jsonWithDiscardedSkills = CcpOtherConstants.EMPTY_JSON
					.put(Fields.discardedSkills, discardedSkills);
					CcpJsonRepresentation jsonWithExcludedSkills = jsonWithDiscardedSkills
					.put(Fields.excludedSkill, excludedSkill);

					CcpJsonRepresentation response = jsonWithExcludedSkills
					.put(VisJsonCommonsFields.skill, skills)
;
			return response;
		}
		
		private CcpJsonRepresentation putLabel(CcpJsonRepresentation json) {
			String skill = json.getAsString(VisJsonCommonsFields.skill);
			String word = json.getAsString(VisJsonCommonsFields.word);

			boolean sameWord = skill.equals(word);
			if(sameWord) {
				CcpJsonRepresentation labeledSkill = json.put(VisJsonCommonsFields.label, skill);
				return labeledSkill;
			}
			String labelPrefix = word + " (";
			String labelWithoutClosing = labelPrefix + skill;
			String label = labelWithoutClosing + ")";
			CcpJsonRepresentation labeledSkill = json.put(VisJsonCommonsFields.label, label);
			return labeledSkill;
		}
	}
	;
	
	static int getWordStatus(CcpJsonRepresentation group, String word) {
		String initials = word.substring(0,2);
		CcpFieldName initialsFieldName = new CcpFieldName(initials);
		boolean containsInitials = group.containsAllFields(initialsFieldName);
		boolean notContainsInitials = false == containsInitials;

		if(notContainsInitials) {
			return 1;
		}
		CcpFieldName sameInitialsFieldName = new CcpFieldName(initials);
		Set<String> wordsWithTheseInitials = group.getAsObject(sameInitialsFieldName);
		boolean containsWord = wordsWithTheseInitials.contains(word);
		boolean notContains = false == containsWord;
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
