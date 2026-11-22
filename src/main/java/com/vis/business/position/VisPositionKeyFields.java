package com.vis.business.position;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityPosition;

/**
 * The input of the actions over one position of the logged-in user (delete and inactivate): the e-mail of the session,
 * the recruiter of the position, and the title; together they are the primary key of the position.
 */
public enum VisPositionKeyFields implements CcpJsonFieldName {
	/** The e-mail of the logged-in user, the recruiter of the position. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	/** The title of the position. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	@CcpJsonFieldValidatorRequired
	title,
	;
}
