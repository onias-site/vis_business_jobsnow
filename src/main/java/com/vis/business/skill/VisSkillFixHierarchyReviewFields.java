package com.vis.business.skill;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Fields of the review of a skill hierarchy fix request. {@code reviewDecisions} is the list of decisions
 * taken by the support bot operator, one per item, each with the {@code type} and the {@code skill} of the
 * item, the {@code decision} ({@link VisSkillFixHierarchyDecisions}) and the operator's {@code justification};
 * {@code reviewSummary} is those decisions written for the email sent to the user.
 */
public enum VisSkillFixHierarchyReviewFields implements CcpJsonFieldName{
	/** The {@code reviewDecisions} field. */
	reviewDecisions,
	/** The {@code decision} field. */
	decision,
	/** The {@code justification} field. */
	justification,
	/** The {@code reviewSummary} field. */
	reviewSummary
}
