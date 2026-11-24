package com.vis.json.fields.validation;

/**
 * Fields accepted by the {@code field} of each item of the {@code sort} of the position ({@code VisJsonFieldsPositionSort}):
 * the criteria by which the resumes found for the position are sorted. Since 2026-11-23 each item names one criterion,
 * and the criteria of one language or of one skill name it in the {@code value} of the item.
 */
public enum VisPositionSortFieldTypes {
	/** By the value as a contractor (PJ). */
	pj,
	/** By the salary as an employee (CLT). */
	clt,
	/** By the level of the language named in the {@code value} of the item. */
	spokenLanguage,
	/** By the years of experience. */
	experience,
	/** By how many skills the resume has. */
	skillsTotal,
	/** By how many of the skills the position would like ({@code shouldSkills}) the resume has. */
	shouldSkillsTotal,
	/** By the days the candidate needs to start. */
	disponibility,
	/** By the months of temporary job the candidate accepts. */
	temporallyJobTime,
	/** By having the skill named in the {@code value} of the item. */
	skill
	;
}
