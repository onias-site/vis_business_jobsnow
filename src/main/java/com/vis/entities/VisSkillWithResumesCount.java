package com.vis.entities;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Function of {@link VisEntitySkill}: adds to a skill of the synonyms file how many resumes mention it or one of its
 * synonyms.
 */
class VisSkillWithResumesCount implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	/** The lines of the file with the word count per resume ({@code <word> = <count>}). */
	private final List<String> lines;

	/**
	 * Keeps the lines of the count file.
	 * @param lines the lines of the file with the word count per resume
	 */
	VisSkillWithResumesCount(List<String> lines) {
		this.lines = lines;
	}

	/**
	 * Adds the count of resumes to the skill.
	 * @param json the skill, with its synonyms
	 * @return the skill with the count of resumes
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		int resumesCount = VisEntitySkill.getResumesCount(json, this.lines);

		CcpJsonRepresentation jsonWithResumesCount = json.put(VisJsonCommonsFields.resumesCount, resumesCount);

		return jsonWithResumesCount;
	}
}
