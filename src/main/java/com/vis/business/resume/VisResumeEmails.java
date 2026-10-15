package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * The e-mail of a resume as the rules of the LinkedIn address need it: the hash, kept in the e-mails of the address in
 * {@link com.vis.entities.VisEntityResumeLinkedin}, and the readable one, for the notice to the support operator.
 */
class VisResumeEmails {

	private VisResumeEmails() {}

	/**
	 * Hashes the e-mail of the JSON when it is still readable; a JSON that went through the transformer of the entity
	 * already carries the hash in {@code email} and the readable e-mail in {@code originalEmail}.
	 * @param json the resume or its key
	 * @return the JSON with the hash in {@code email} and the readable e-mail in {@code originalEmail}
	 */
	static CcpJsonRepresentation withHashedEmail(CcpJsonRepresentation json) {
		boolean emailAlreadyHashed = json.containsField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonRepresentation jsonWithHashedEmail = emailAlreadyHashed ? json : json.getTransformedJson(JnJsonTransformersFieldsEntityDefault.email);
		return jsonWithHashedEmail;
	}
}
