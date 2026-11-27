package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualSkillGrouper;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisResumeSkillCategories;

/**
 * Produces the groups of {@code VisEntityVirtualSkillGrouper} a resume is a child of: one per skill the resume has,
 * each once, directly or indirectly. Direct: the {@code skill} of each item whose {@code category} is
 * {@code skillsFromText}. Indirect: the {@code parent} list of those items (the ancestors of the skill) and the
 * {@code skill} of each item whose {@code category} is {@code parent}, which repeat each other in the resumes saved by
 * the form. The excluded and the discarded skills are left out.
 */
public class VisResumeSkillCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeSkillCombinations INSTANCE = new VisResumeSkillCombinations();

	private VisResumeSkillCombinations() {}

	/**
	 * One combination per skill of the resume, direct or indirect.
	 * @param json the resume
	 * @return the combinations, {@code {"skill": <skill>}}, none when the resume has no skill
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		boolean noSkill = false == json.containsField(VisEntityResume.Fields.skill);

		if(noSkill) {
			return List.of();
		}

		List<CcpJsonRepresentation> skills = json.getAsJsonList(VisEntityResume.Fields.skill);
		String directCategory = VisResumeSkillCategories.skillsFromText.name();
		String parentCategory = VisResumeSkillCategories.parent.name();
		Set<String> skillNames = new LinkedHashSet<>();
		for (CcpJsonRepresentation skill : skills) {
			String category = skill.getAsString(VisJsonFieldsSkills.category);
			boolean direct = directCategory.equals(category);
			boolean indirect = parentCategory.equals(category);
			boolean otherCategory = false == direct && false == indirect;

			if(otherCategory) {
				continue;
			}

			String skillName = skill.getAsString(VisJsonFieldsSkills.skill);
			skillNames.add(skillName);
			List<String> ancestors = skill.getAsStringList(VisJsonFieldsSkills.parent);
			skillNames.addAll(ancestors);
		}
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String skillName : skillNames) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualSkillGrouper.Fields.skill, skillName);
			combinations.add(combination);
		}
		return combinations;
	}
}
