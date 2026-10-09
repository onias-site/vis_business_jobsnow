package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Fields of the answers of {@link VisServiceSkillSuggestion}: {@code status} is added by
 * {@link VisServiceSkillSuggestion#GetSkillSuggestion}; the other two items answer with the json they received, so
 * their search procedure returns no field ({@code inexistentField}).
 */
enum VisSkillSuggestionResponse implements CcpJsonFieldName{
	/** The {@code status} field. */
	status,
	/** The {@code inexistentField} field. */
	inexistentField
}
