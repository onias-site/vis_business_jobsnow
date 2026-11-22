package com.vis.business.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.telegram.VisSkillWordsInMemory;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The broad skills of a text (since 2026-11-22): a skill found in the text that has skills below it in the hierarchy, none
 * of them found in the same text. A position asking for a "backend developer" names a term too broad, which says nothing
 * of java, .net or php: the search of resumes takes every skill below it as an alternative (see
 * {@code VisBusinessResumesOfTheFilters}), and the screen of the position shows the term with those skills, the
 * specifications of it. A position that also names php (or any skill below backend) has specified the term, and backend
 * is not broad there.
 * <p>
 * Runs over the answer of {@code skills/fromText}: the skills found ({@code skill}) and the ones the screen sent as
 * excluded ({@code excludedSkill}, the required skills of the position) together are the skills of the text.
 */
public class VisBusinessPutTheBroadSkills implements CcpBusiness {

	/** Fields of the broad skills. */
	enum Fields implements CcpJsonFieldName {
		/** The skills the screen sent as excluded, echoed back by the search in the text. */
		excludedSkill,
		/** The broad skills of the text, each one with {@code skill} and {@code children}. */
		broadSkill,
		/** Every skill below the broad one in the hierarchy, direct or not, in alphabetical order. */
		children
	}

	/** The dictionary of skills in memory, with the hierarchy. */
	private final VisSkillWordsInMemory words;

	/**
	 * Keeps the dictionary.
	 * @param words the dictionary of skills in memory
	 */
	public VisBusinessPutTheBroadSkills(VisSkillWordsInMemory words) {
		this.words = words;
	}

	/**
	 * Adds the broad skills of the text.
	 * @param json the answer of the search of skills in the text
	 * @return the same answer with {@code broadSkill} (empty when the text has none)
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Set<String> skillsOfTheText = new LinkedHashSet<>();
		List<CcpJsonRepresentation> foundSkills = json.getAsJsonList(VisJsonCommonsFields.skill);
		List<CcpJsonRepresentation> excludedSkills = json.getAsJsonList(Fields.excludedSkill);
		addTheNames(skillsOfTheText, foundSkills);
		addTheNames(skillsOfTheText, excludedSkills);

		List<CcpJsonRepresentation> broadSkills = new ArrayList<>();
		for (String skill : skillsOfTheText) {
			Set<String> skillAndItsDescendants = this.words.getSkillAndItsDescendants(skill);
			Set<String> descendants = new LinkedHashSet<>(skillAndItsDescendants);
			descendants.remove(skill);
			boolean noSkillBelow = descendants.isEmpty();

			if(noSkillBelow) {
				continue;
			}

			boolean specified = isAnyOfThemInTheText(descendants, skillsOfTheText);

			if(specified) {
				continue;
			}

			List<String> children = new ArrayList<>(descendants);
			Collections.sort(children);
			CcpJsonRepresentation broadSkill = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.skill, skill)
					.put(Fields.children, children);
			broadSkills.add(broadSkill);
		}
		CcpJsonRepresentation withTheBroadSkills = json.put(Fields.broadSkill, broadSkills);
		return withTheBroadSkills;
	}

	private static void addTheNames(Set<String> names, List<CcpJsonRepresentation> skills) {
		for (CcpJsonRepresentation skill : skills) {
			String name = skill.getAsString(VisJsonCommonsFields.skill);
			String upperCaseName = name.toUpperCase();
			names.add(upperCaseName);
		}
	}

	private static boolean isAnyOfThemInTheText(Set<String> descendants, Set<String> skillsOfTheText) {
		for (String descendant : descendants) {
			boolean inTheText = skillsOfTheText.contains(descendant);

			if(inTheText) {
				return true;
			}
		}
		return false;
	}
}
