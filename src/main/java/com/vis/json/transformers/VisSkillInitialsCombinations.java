package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntityVirtualSkillInitialsGrouper;

/**
 * Produces the groups of {@code VisEntityVirtualSkillInitialsGrouper} a skill belongs to: one per distinct first two
 * letters (upper case) among its words, which are the skill itself, each synonym and each parent. A word shorter than
 * two characters has no group.
 */
public class VisSkillInitialsCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisSkillInitialsCombinations INSTANCE = new VisSkillInitialsCombinations();

	private VisSkillInitialsCombinations() {}

	/**
	 * The words of the skill.
	 * @param skill the record of {@link VisEntitySkill}
	 * @return the skill itself, its synonyms and its parents
	 */
	public static List<String> getWords(CcpJsonRepresentation skill) {
		List<String> words = new ArrayList<>();
		String skillName = skill.getAsString(VisEntitySkill.Fields.skill);
		words.add(skillName);
		List<String> synonyms = skill.getAsStringList(VisEntitySkill.Fields.synonym);
		words.addAll(synonyms);
		List<String> parents = skill.getAsStringList(VisEntitySkill.Fields.parent);
		words.addAll(parents);
		return words;
	}

	/**
	 * One combination per distinct initials of the words of the skill.
	 * @param json the record of {@link VisEntitySkill}
	 * @return the combinations
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		List<String> words = getWords(json);
		Set<String> parentIds = new LinkedHashSet<>();
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String word : words) {
			int wordLength = word.length();
			boolean wordIsTooShort = wordLength < VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD;

			if(wordIsTooShort) {
				continue;
			}

			String parentId = VisEntityVirtualSkillInitialsGrouper.getParentId(word);
			boolean newGroup = parentIds.add(parentId);
			boolean groupAlreadyCombined = false == newGroup;

			if(groupAlreadyCombined) {
				continue;
			}

			CcpJsonRepresentation combination = VisEntityVirtualSkillInitialsGrouper.getCombination(word);
			combinations.add(combination);
		}
		return combinations;
	}
}
