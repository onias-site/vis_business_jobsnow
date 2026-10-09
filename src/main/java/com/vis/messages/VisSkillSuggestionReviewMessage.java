package com.vis.messages;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Values of the email that tells the candidate the result of the review of a skill suggestion, shared by the
 * templates of the approval and of the rejection. The json arrives from the transfer of the suggestion, already
 * transformed (email as hash), with the support bot operator's {@code explanation}.
 */
final class VisSkillSuggestionReviewMessage {

	/** Utility class; not instantiable. */
	private VisSkillSuggestionReviewMessage() {}

	/**
	 * Puts back the readable email (the address the email goes to) and escapes the {@code explanation}, which the
	 * operator typed in the bot and goes into an HTML email body.
	 * @param json the reviewed suggestion
	 * @return the values of the template
	 */
	static CcpJsonRepresentation prepare(CcpJsonRepresentation json) {
		CcpJsonRepresentation jsonWithOriginalEmail = json
				.renameField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail, JnJsonCommonsFields.email);
		String explanation = json.getAsString(JnJsonCommonsFields.explanation);
		String withoutAmpersand = explanation.replace("&", "&amp;");
		String withoutLessThan = withoutAmpersand.replace("<", "&lt;");
		String escapedExplanation = withoutLessThan.replace(">", "&gt;");
		CcpJsonRepresentation jsonWithEscapedExplanation = jsonWithOriginalEmail.put(JnJsonCommonsFields.explanation, escapedExplanation);
		return jsonWithEscapedExplanation;
	}
}
