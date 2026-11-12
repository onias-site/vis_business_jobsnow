package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisResumeSkillTypes;

/**
 * A field of the child of the groups of ddd (of the resumes and of their views, {@code childFieldsProducers}):
 * {@code skill}, the names of the skills of the resume whose {@code type} is {@code skillsFromResume}, in the order of
 * the list ({@code skill.filter(x -> x.type == 'skillsFromResume').map(x -> x.skill)}).
 */
public class VisResumeSkillsFromResumeChildField implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeSkillsFromResumeChildField INSTANCE = new VisResumeSkillsFromResumeChildField();

	private VisResumeSkillsFromResumeChildField() {}

	/**
	 * The skills of the resume itself.
	 * @param json the resume (or the view, with the skill list of the resume)
	 * @return {@code {"skill": [<skill>, ...]}}, an empty list when there is none
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<CcpJsonRepresentation> skills = json.getAsJsonList(VisEntityResume.Fields.skill);
		String skillsFromResume = VisResumeSkillTypes.skillsFromResume.name();
		List<String> skillNames = new ArrayList<>();
		for (CcpJsonRepresentation skill : skills) {
			String type = skill.getAsString(VisJsonFieldsSkills.type);
			boolean notFromResume = false == skillsFromResume.equals(type);

			if(notFromResume) {
				continue;
			}

			String skillName = skill.getAsString(VisJsonFieldsSkills.skill);
			skillNames.add(skillName);
		}
		CcpJsonRepresentation childField = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.skill, skillNames);
		return childField;
	}
}
