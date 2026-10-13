package com.vis.json.fields.validation;

/**
 * Types accepted by the {@code type} field of each skill of the resume ({@code VisJsonFieldsSkills}). Outside the
 * {@code discardedSkills} category the type repeats the category; inside it, the type is the reason of the discard.
 */
public enum VisResumeSkillTypes {

	/** Type of the category {@code skillsFromText}. */
	skillsFromResume,
	/** Same as the category {@code excludedSkills}. */
	excludedSkills,
	/** Same as the category {@code parent}. */
	parent,
	/** Discarded: the word is a piece of another word of the text, too short to count alone. */
	isPieceOfOtherWord,
	/** Discarded: the word is contained in a longer skill also found in the text. */
	isPieceOfOtherSkill,
	/** Discarded: the skill was already chosen through another word. */
	skillAlreadyAdded
	;
}
