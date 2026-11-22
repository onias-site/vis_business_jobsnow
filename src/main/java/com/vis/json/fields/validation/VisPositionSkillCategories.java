package com.vis.json.fields.validation;

import java.util.Arrays;
import java.util.List;

/**
 * Categories accepted by the {@code category} field of each skill of the position ({@code VisJsonFieldsPositionSkills}):
 * the same of the resume ({@code VisResumeSkillCategories}) with {@code shouldSkills} in place of
 * {@code excludedSkills}. Each category declares the {@code type} values it accepts.
 */
public enum VisPositionSkillCategories {

	/** Main list: the skills the position requires; they take part in the hashes of the position. */
	skillsFromText(VisPositionSkillTypes.skillsFromResume),
	/** The skills the position would like, not required: they count in the sorting ({@code shouldSkillsTotal}). */
	shouldSkills(VisPositionSkillTypes.shouldSkills),
	/** Implicit skills: the parents of the skills of the main list that are not in it. */
	parent(VisPositionSkillTypes.parent),
	/** Skills discarded by the search of skills in the text; the reason is in the {@code type}. */
	discardedSkills(VisPositionSkillTypes.isPieceOfOtherWord, VisPositionSkillTypes.isPieceOfOtherSkill, VisPositionSkillTypes.skillAlreadyAdded)
	;

	/** The types this category accepts. */
	private final List<VisPositionSkillTypes> acceptedTypes;

	/**
	 * Associates the category with the types it accepts.
	 * @param acceptedTypes the accepted types
	 */
	private VisPositionSkillCategories(VisPositionSkillTypes... acceptedTypes) {
		this.acceptedTypes = Arrays.asList(acceptedTypes);
	}

	/**
	 * Tells whether the type may go with this category.
	 * @param type the type
	 * @return {@code true} when the category accepts the type
	 */
	public boolean accepts(VisPositionSkillTypes type) {
		boolean accepted = this.acceptedTypes.contains(type);
		return accepted;
	}

	/**
	 * Returns the types this category accepts.
	 * @return the accepted types
	 */
	public List<VisPositionSkillTypes> getAcceptedTypes() {
		return this.acceptedTypes;
	}
}
