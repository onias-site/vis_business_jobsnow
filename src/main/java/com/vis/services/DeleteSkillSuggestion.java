package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Body validation of {@link VisServiceSkillSuggestion#DeleteSkillSuggestion}: the same primary key as the lookup
 * (email + skill).
 */
enum DeleteSkillSuggestion implements CcpJsonFieldName{
	/** The {@code email} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	skill,
}
