package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualExcludedSkillCounter;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisResumeSkillCategories;

/**
 * Produces the groups of {@code VisEntityVirtualExcludedSkillCounter} a resume is counted in: one per skill its owner
 * excluded (each item of the {@code skill} list whose {@code category} is {@code excludedSkills}), the {@code skill} of
 * the item, each once.
 */
public class VisResumeExcludedSkillCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeExcludedSkillCombinations INSTANCE = new VisResumeExcludedSkillCombinations();

	private VisResumeExcludedSkillCombinations() {}

	/**
	 * One combination per excluded skill of the resume.
	 * @param json the resume
	 * @return the combinations, {@code {"skill": <skill>}}, none when the resume excludes no skill
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
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
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String excludedSkill : excludedSkills) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualExcludedSkillCounter.Fields.skill, excludedSkill);
			combinations.add(combination);
		}
		return combinations;
	}
}
