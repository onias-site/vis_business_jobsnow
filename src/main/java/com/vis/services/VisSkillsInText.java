package com.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Finds the known skills in a free text: the logic of {@link VisServiceSkills#GetSkillsFromText}, apart from where the
 * words of the skills come from, so that the load of the positions of Telegram finds them with no read per message.
 */
public final class VisSkillsInText {

	/** Utility class; not instantiable. */
	private VisSkillsInText() {}

	/**
	 * Finds the skills of the text.
	 * @param json {@code text} and the {@code excludedSkill} list
	 * @param wordsOfTheInitials gives the items of the words of the skills whose initials are the ones of the given words
	 * @return {@code skill} (the chosen skills), {@code discardedSkills} and {@code excludedSkill}
	 */
	public static CcpJsonRepresentation find(CcpJsonRepresentation json, Function<Collection<String>, List<CcpJsonRepresentation>> wordsOfTheInitials) {
		String rawText = json.getAsString(Fields.text);
		String text = rawText.toUpperCase();
		String trimmedText = text.trim();
	
		boolean emptyText = trimmedText.isEmpty();
		
		if(emptyText) {
			return CcpOtherConstants.EMPTY_JSON;
		}
		
		String[] phrases = text.split(CcpOtherConstants.DELIMITERS);
		List<String> phrasesOfTheText = Arrays.asList(phrases);

		// the skills whose words start like the words of the text
		List<CcpJsonRepresentation> wordsOfTheSkills = wordsOfTheInitials.apply(phrasesOfTheText);

		List<CcpJsonRepresentation> allSkillsFoundInTheText = new ArrayList<>();

		for (CcpJsonRepresentation skill : wordsOfTheSkills) {
			String word = skill.getAsString(VisJsonCommonsFields.word);
			boolean found = text.contains(word);
			if(found) {
				allSkillsFoundInTheText.add(skill);
				continue;
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
				
				CcpJsonRepresentation labeledSkill = putLabel(skill);
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
			CcpJsonRepresentation labeledSkill = putLabel(skill);
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
		
		Collection<CcpJsonRepresentation> chosenSkills = chosenSkillsByName.values();
		List<CcpJsonRepresentation> skills = new ArrayList<>(chosenSkills);
		CcpJsonRepresentation jsonWithDiscardedSkills = CcpOtherConstants.EMPTY_JSON
				.put(Fields.discardedSkills, discardedSkills);
				CcpJsonRepresentation jsonWithExcludedSkills = jsonWithDiscardedSkills
				.put(Fields.excludedSkill, excludedSkill);

				CcpJsonRepresentation response = jsonWithExcludedSkills
				.put(VisJsonCommonsFields.skill, skills)
;
		return response;
	}
	
	/**
	 * Puts the label of the skill: the skill itself, or {@code word (skill)} when found by another word.
	 * @param json the skill found
	 * @return the skill with {@code label}
	 */
	private static CcpJsonRepresentation putLabel(CcpJsonRepresentation json) {
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
