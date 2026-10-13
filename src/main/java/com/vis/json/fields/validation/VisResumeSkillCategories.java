package com.vis.json.fields.validation;

import java.util.Arrays;
import java.util.List;

/**
 * Categories accepted by the {@code category} field of each skill of the resume ({@code VisJsonFieldsSkills}): the list
 * of the skills tab the skill belongs to. Each category declares the {@code type} values it accepts.
 */
public enum VisResumeSkillCategories {

	/** Main list: the skills by which the candidate wants the resume to be found. */
	skillsFromText(VisResumeSkillTypes.skillsFromResume),
	/** The skills by which the candidate does not want the resume to be found. */
	excludedSkills(VisResumeSkillTypes.excludedSkills),
	/** Implicit skills: the parents of the skills of the main list that are not in it. */
	parent(VisResumeSkillTypes.parent),
	/** Skills discarded by the search of skills in the resume text; the reason is in the {@code type}. */
	discardedSkills(VisResumeSkillTypes.isPieceOfOtherWord, VisResumeSkillTypes.isPieceOfOtherSkill, VisResumeSkillTypes.skillAlreadyAdded)
	;

	/** The types this category accepts. */
	private final List<VisResumeSkillTypes> acceptedTypes;

	/**
	 * Associates the category with the types it accepts.
	 * @param acceptedTypes the accepted types
	 */
	private VisResumeSkillCategories(VisResumeSkillTypes... acceptedTypes) {
		this.acceptedTypes = Arrays.asList(acceptedTypes);
	}

	/**
	 * Tells whether the type may go with this category.
	 * @param type the type
	 * @return {@code true} when the category accepts the type
	 */
	public boolean accepts(VisResumeSkillTypes type) {
		boolean accepted = this.acceptedTypes.contains(type);
		return accepted;
	}

	/**
	 * Returns the types this category accepts.
	 * @return the accepted types
	 */
	public List<VisResumeSkillTypes> getAcceptedTypes() {
		return this.acceptedTypes;
	}
}
