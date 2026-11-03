package com.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Finds the known skills in a free text: the logic of {@link VisServiceSkills#GetSkillsFromText}, apart from where the
 * words of the skills come from, so that the load of the positions of Telegram finds them with no read per message.
 */
public final class VisSkillsInText {

	/** The texts of the reading. */
	enum Constants implements CcpJsonFieldName {
		/** Joins two words that are also searched one by one ({@code SCRUM/KANBAN}). */
		SLASH { public String getValue() { return "/"; } },
	}

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
		
		String[] wordsOfTheText = text.split(CcpOtherConstants.DELIMITERS);
		List<String> phrasesOfTheText = withTheSlashedParts(wordsOfTheText);
		String[] phrases = phrasesOfTheText.toArray(new String[0]);

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
		
		List<String> tokensOfTheText = getTokens(text);
		List<String> compoundSkillWords = getCompoundSkillWordsInTheText(allSkillsFoundInTheText, tokensOfTheText, excluded);
		Set<Integer> positionsOfTheCompoundWords = getPositionsOfTheCompoundWords(compoundSkillWords, tokensOfTheText);

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
				List<String> tokensOfTheWord = getTokens(word);
				boolean compoundWord = tokensOfTheWord.size() > 1;

				if(compoundWord) {
					boolean notWholeWordsInTheText = false == compoundSkillWords.contains(word);

					if(notWholeWordsInTheText) {
						continue;
					}
					CcpJsonRepresentation labeledCompoundSkill = putLabel(skill);
					choosedSkills.add(labeledCompoundSkill);
					continue;
				}

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

				String slashedSkillWord = getSlashedSkillWord(cleanWord, wordsOfTheText, allSkillsFoundInTheText);
				boolean onlyPartOfASlashedSkillWord = false == slashedSkillWord.isEmpty();

				if(onlyPartOfASlashedSkillWord) {
					CcpJsonRepresentation pieceOfOtherSkill = jsonPiece.put(Fields.associated, slashedSkillWord);
					discardedSkills = discardedSkills.addToList(Fields.isPieceOfOtherSkill, pieceOfOtherSkill);
					continue;
				}

				String compoundSkillWord = getCompoundSkillWord(cleanWord, tokensOfTheText, compoundSkillWords, positionsOfTheCompoundWords);
				boolean onlyPartOfACompoundSkillWord = false == compoundSkillWord.isEmpty();

				if(onlyPartOfACompoundSkillWord) {
					CcpJsonRepresentation pieceOfOtherSkill = jsonPiece.put(Fields.associated, compoundSkillWord);
					discardedSkills = discardedSkills.addToList(Fields.isPieceOfOtherSkill, pieceOfOtherSkill);
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
	 * The words of the text plus the parts of each word that has a slash: "/" is not a delimiter, so that
	 * {@code PL/SQL}, {@code CI/CD} and {@code S/4HANA} stay whole, but in {@code SCRUM/KANBAN} or {@code REST/SOAP} each
	 * part is also a word of its own, both to choose the skills by their initials and to be an independent word.
	 * @param wordsOfTheText the text split by the delimiters
	 * @return the words, each slashed one followed by its parts
	 */
	private static List<String> withTheSlashedParts(String[] wordsOfTheText) {
		String slash = Constants.SLASH.getValue();
		String slashPattern = Pattern.quote(slash);
		List<String> words = new ArrayList<>();
		for (String word : wordsOfTheText) {
			words.add(word);
			boolean noSlash = false == word.contains(slash);

			if(noSlash) {
				continue;
			}

			String[] parts = word.split(slashPattern);
			for (String part : parts) {
				boolean emptyPart = part.isEmpty();

				if(emptyPart) {
					continue;
				}

				words.add(part);
			}
		}
		return words;
	}

	/**
	 * The word of a found skill, written with a slash in the text, that a short word is only a part of: {@code SQL} in a
	 * text that has {@code PL/SQL} but no {@code SQL} alone, so that the part does not become a skill of its own.
	 * @param cleanWord the short word, without delimiters
	 * @param wordsOfTheText the text split by the delimiters, without the slashed parts
	 * @param skillsFound the skills whose words are in the text
	 * @return the slashed word, or empty when the short word stands alone in the text or is part of no slashed skill word
	 */
	private static String getSlashedSkillWord(String cleanWord, String[] wordsOfTheText, List<CcpJsonRepresentation> skillsFound) {
		List<String> words = Arrays.asList(wordsOfTheText);
		boolean standsAlone = words.contains(cleanWord);

		if(standsAlone) {
			return "";
		}

		String slash = Constants.SLASH.getValue();
		String slashPattern = Pattern.quote(slash);
		for (CcpJsonRepresentation skill : skillsFound) {
			String skillWord = skill.getAsString(VisJsonCommonsFields.word);
			boolean notInTheTextAsAWord = false == words.contains(skillWord);

			if(notInTheTextAsAWord) {
				continue;
			}

			boolean noSlash = false == skillWord.contains(slash);

			if(noSlash) {
				continue;
			}

			String[] parts = skillWord.split(slashPattern);
			List<String> partsOfTheSkillWord = Arrays.asList(parts);
			boolean isAPart = partsOfTheSkillWord.contains(cleanWord);

			if(isAPart) {
				return skillWord;
			}
		}
		return "";
	}

	/**
	 * The words of a text split by the delimiters, without the empty ones.
	 * @param text the text
	 * @return the words, in the order of the text
	 */
	private static List<String> getTokens(String text) {
		String[] split = text.split(CcpOtherConstants.DELIMITERS);
		Stream<String> splitStream = Arrays.stream(split);
		Stream<String> nonEmptyStream = splitStream.filter(token -> false == token.isEmpty());
		List<String> tokens = nonEmptyStream.collect(Collectors.toList());
		return tokens;
	}

	/**
	 * The short skill words written with delimiters ({@code SAP FI}, {@code VB.NET}, {@code MS SQL}) that are in the text
	 * as a sequence of whole words: the delimiters split them, so they are never one word of the text.
	 * @param skillsFound the skills whose words are in the text
	 * @param tokensOfTheText the words of the text
	 * @param excluded the excluded words
	 * @return the compound words in the text
	 */
	private static List<String> getCompoundSkillWordsInTheText(List<CcpJsonRepresentation> skillsFound, List<String> tokensOfTheText, List<String> excluded) {
		List<String> compoundSkillWords = new ArrayList<>();
		for (CcpJsonRepresentation skill : skillsFound) {
			String skillWord = skill.getAsString(VisJsonCommonsFields.word);
			String word = skillWord.toUpperCase();
			boolean longWord = word.length() >= 7;

			if(longWord) {
				continue;
			}

			boolean excludedWord = excluded.contains(word);

			if(excludedWord) {
				continue;
			}

			List<String> tokensOfTheWord = getTokens(word);
			boolean singleWord = tokensOfTheWord.size() < 2;

			if(singleWord) {
				continue;
			}

			int position = Collections.indexOfSubList(tokensOfTheText, tokensOfTheWord);
			boolean notInTheText = position < 0;

			if(notInTheText) {
				continue;
			}

			compoundSkillWords.add(word);
		}
		return compoundSkillWords;
	}

	/**
	 * The positions of the words of the text that belong to a compound skill word found.
	 * @param compoundSkillWords the compound words in the text
	 * @param tokensOfTheText the words of the text
	 * @return the positions covered by the compound words
	 */
	private static Set<Integer> getPositionsOfTheCompoundWords(List<String> compoundSkillWords, List<String> tokensOfTheText) {
		Set<Integer> positions = new HashSet<>();
		int textSize = tokensOfTheText.size();
		for (String compoundSkillWord : compoundSkillWords) {
			List<String> tokensOfTheWord = getTokens(compoundSkillWord);
			int wordSize = tokensOfTheWord.size();
			for (int start = 0; start + wordSize <= textSize; start++) {
				List<String> window = tokensOfTheText.subList(start, start + wordSize);
				boolean sameWords = window.equals(tokensOfTheWord);

				if(false == sameWords) {
					continue;
				}

				for (int position = start; position < start + wordSize; position++) {
					positions.add(position);
				}
			}
		}
		return positions;
	}

	/**
	 * The compound skill word that a short word is only a part of: {@code SQL} in a text that has {@code MS SQL} but no
	 * {@code SQL} out of it, so that the part does not become a skill of its own.
	 * @param cleanWord the short word, without delimiters
	 * @param tokensOfTheText the words of the text
	 * @param compoundSkillWords the compound words in the text
	 * @param positionsOfTheCompoundWords the positions covered by the compound words
	 * @return the compound word, or empty when the short word also stands out of every compound word or is part of none
	 */
	private static String getCompoundSkillWord(String cleanWord, List<String> tokensOfTheText, List<String> compoundSkillWords, Set<Integer> positionsOfTheCompoundWords) {
		int textSize = tokensOfTheText.size();
		for (int position = 0; position < textSize; position++) {
			String token = tokensOfTheText.get(position);
			boolean otherWord = false == token.equals(cleanWord);

			if(otherWord) {
				continue;
			}

			boolean covered = positionsOfTheCompoundWords.contains(position);

			if(false == covered) {
				return "";
			}
		}

		for (String compoundSkillWord : compoundSkillWords) {
			List<String> tokensOfTheWord = getTokens(compoundSkillWord);
			boolean isAPart = tokensOfTheWord.contains(cleanWord);

			if(isAPart) {
				return compoundSkillWord;
			}
		}
		return "";
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
