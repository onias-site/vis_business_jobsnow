package com.vis.services;

import java.util.Set;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.vis.business.skill.VisSkillWordsByInitials;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/** Fields of the skills services. */
enum Fields implements CcpJsonFieldName{
	/** The {@code text} field. */
	text,
	/** The {@code excludedSkill} field. */
	excludedSkill,
	/** The {@code label} field. */
	label, 
	/** The {@code discardedSkills} field. */
	discardedSkills,
	/** The {@code isPieceOfOtherWord} field. */
	isPieceOfOtherWord,
	/** The {@code associated} field. */
	associated,
	/** The {@code isPieceOfOtherSkill} field. */
	isPieceOfOtherSkill, 
	/** The {@code skillAlreadyAdded} field. */
	skillAlreadyAdded 
}

/**
 * Service for skill operations: extraction of skills from free text.
 * Holds the richest logic of the skills module. The hierarchy fix lives in
 * {@link VisServiceSkillFixHierarchy} and the skill suggestions in {@link VisServiceSkillSuggestion}.
 */
public enum VisServiceSkills implements JnService {
	
	/**
	 * Finds the known skills in a free text (upper case): words of 7 characters or more are found anywhere, unless they are
	 * part of a longer skill found; shorter words only as whole words. Excluded skills are ignored, and each skill is kept
	 * once (the shortest label). The discarded ones come back with the reason. The known words come from the groups of
	 * the initials of the words of the text ({@link VisSkillWordsByInitials}), each skill with its synonyms.
	 */
	GetSkillsFromText{

		/**
		 * Finds the skills of the text.
		 * @param json {@code text} and the {@code excludedSkill} list
		 * @return {@code skill} (the chosen skills), {@code discardedSkills} and {@code excludedSkill}
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation skillsOfTheText = VisSkillsInText.find(json, VisSkillWordsByInitials::getItems);
			return skillsOfTheText;
		}
	}
	;
	
	/**
	 * Tells whether a word is in the group of its first two letters.
	 * @param group the words by initials
	 * @param word the word
	 * @return 0 when present, 1 when there is no group, 2 when the group does not have the word
	 */
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

	/** Input rules of {@link VisServiceSkills#GetSkillsFromText}. */
	enum GetSkillsFromText implements CcpJsonFieldName{
		/** The {@code text} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(maxLength = 5_000_000, allowsEmptyString = true)
		text,
		/** The {@code excludedSkill} field: list, nested JSON. */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeNestedJson(jsonValidation = ExcludedSkillFields.class)
		excludedSkill
	}
	
	
	/** Rules of an excluded skill. */
	enum ExcludedSkillFields implements CcpJsonFieldName{
		
		/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		skill, 

		/** The {@code word} field: validated as in {@code VisJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		word 
	
	}
