package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisResumeSkillCategories;

/**
 * The skills the owner of a resume excluded (each item of the {@code skill} list whose {@code category} is
 * {@code excludedSkills}), each once: the keys of the counter of the most avoided skills
 * ({@code counterKeyClass} of a {@code @CcpEntityGrouperChildCounter} counted by
 * {@code JnGrouperChildCurrentCounterEntityBeforeWrite}), and the groups of {@code VisResumeExcludedSkillCombinations}.
 */
public class VisAvoidedSkillsCounterKeys implements Function<CcpJsonRepresentation, List<String>>{
	/** The single instance: the class keeps no state. */
	public final static VisAvoidedSkillsCounterKeys INSTANCE = new VisAvoidedSkillsCounterKeys();

	private VisAvoidedSkillsCounterKeys() {}

	/**
	 * The excluded skills of the resume.
	 * @param json the resume
	 * @return the skills, none when the resume excludes no skill
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		boolean noSkill = false == json.containsField(VisEntityResume.Fields.skill);

		if(noSkill) {
			return List.of();
		}

		List<CcpJsonRepresentation> skills = json.getAsJsonList(VisEntityResume.Fields.skill);
		String excludedCategory = VisResumeSkillCategories.excludedSkills.name();
		Set<String> excludedSkills = new LinkedHashSet<>();
		for (CcpJsonRepresentation skill : skills) {
			String category = skill.getAsString(VisJsonFieldsSkills.category);
			boolean notExcluded = false == excludedCategory.equals(category);

			if(notExcluded) {
				continue;
			}

			String skillName = skill.getAsString(VisJsonFieldsSkills.skill);
			excludedSkills.add(skillName);
		}
		List<String> keys = new ArrayList<>(excludedSkills);
		return keys;
	}
}
