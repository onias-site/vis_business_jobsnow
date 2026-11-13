package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeRestriction;

/**
 * Deletes the restriction of the resume ({@code vis_resume_restriction}) after an update of the resume, active or
 * inactive (since 2026-11-12): the save only passes with every field valid, so the owner corrected what the restriction
 * asked.
 */
public class VisBusinessReleaseResumeRestriction implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisBusinessReleaseResumeRestriction INSTANCE = new VisBusinessReleaseResumeRestriction();

	private VisBusinessReleaseResumeRestriction() {}

	/**
	 * Deletes the restriction, when there is one.
	 * @param json the updated resume, whose {@code email} may come already hashed, with the readable one in
	 * {@code originalEmail}
	 * @return the resume, as it came
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the restriction hashes the e-mail itself: it gets the readable one
		boolean emailHashed = json.containsField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonFieldName emailField = emailHashed ? JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail : VisEntityResume.Fields.email;
		String email = json.getAsString(emailField);
		CcpJsonRepresentation restrictionKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeRestriction.Fields.email, email);
		VisEntityResumeRestriction.ENTITY.delete(restrictionKey);
		return json;
	}
}
