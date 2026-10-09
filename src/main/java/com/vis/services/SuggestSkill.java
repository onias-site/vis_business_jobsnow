package com.vis.services;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Body validation of {@link VisServiceSkillSuggestion#SuggestSkill}. Each field copies the rules straight from the
 * class that declares them, because {@code CcpJsonCopyFieldValidationsFrom} is not recursive.
 */
enum SuggestSkill implements CcpJsonFieldName{
	/** The {@code email} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	skill,

	/** The {@code synonym} field: validated as in {@code VisJsonCommonsFields}, list. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	synonym,

	/** The {@code description} field: validated as in {@code JnJsonCommonsFields}, required. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	description,
}
