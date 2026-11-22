package com.vis.json.fields.validation;

/**
 * Fields accepted by the {@code field} of each item of the {@code sort} of the position ({@code VisJsonFieldsPositionSort}):
 * the criteria by which the resumes found for the position are sorted.
 */
public enum VisPositionSortFieldTypes {
	/** By the value as a contractor (PJ). */
	pj,
	/** By the salary as an employee (CLT). */
	clt,
	/** By the years of experience. */
	experience,
	/** By how many of the skills the position would like ({@code shouldSkills}) the resume has. */
	shouldSkillsTotal
	;
}
