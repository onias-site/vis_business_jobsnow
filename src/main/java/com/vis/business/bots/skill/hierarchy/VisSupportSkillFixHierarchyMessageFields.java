package com.vis.business.bots.skill.hierarchy;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Placeholders of the {@link VisSupportSkillFixHierarchyMessages} templates that are not fields of the pending
 * request.
 */
public enum VisSupportSkillFixHierarchyMessageFields implements CcpJsonFieldName{
	/** The {@code itemNumber} field. */
	itemNumber,
	/** The {@code itemsCount} field. */
	itemsCount,
	/** The {@code typeDescription} field. */
	typeDescription,
	/** The {@code approvedSkills} field. */
	approvedSkills,
	/** The {@code rejectedSkills} field. */
	rejectedSkills,
	/** The {@code options} field. */
	options,
	/** The {@code ignoreConfirmation} field. */
	ignoreConfirmation,
	/** The {@code skills} field: the skills of one part of the request, separated by commas. */
	skills
}
