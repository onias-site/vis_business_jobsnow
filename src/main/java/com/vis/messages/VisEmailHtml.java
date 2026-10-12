package com.vis.messages;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Text typed by a person (the candidate's justification, the operator's justification) that goes into the HTML body of
 * an email: escaped, so that a {@code <} typed by them shows up as text, and with the line breaks kept.
 */
final class VisEmailHtml {

	/** Utility class; not instantiable. */
	private VisEmailHtml() {}

	/**
	 * Escapes the text for an HTML email body and turns its line breaks into {@code <br>}.
	 * @param text the typed text
	 * @return the escaped text
	 */
	static String escape(String text) {
		String withoutAmpersand = text.replace("&", "&amp;");
		String withoutLessThan = withoutAmpersand.replace("<", "&lt;");
		String withoutGreaterThan = withoutLessThan.replace(">", "&gt;");
		String withLineBreaks = withoutGreaterThan.replace("\n", "<br>");
		return withLineBreaks;
	}

	/**
	 * Adds {@code escapedDescription}: the candidate's justification ({@code description}) escaped for the email body.
	 * The {@code description} itself is kept as typed, because the json may still be read as the request.
	 * @param json the request or suggestion, with {@code description}
	 * @return the json with {@code escapedDescription}
	 */
	static CcpJsonRepresentation putEscapedDescription(CcpJsonRepresentation json) {
		String description = json.getAsString(JnJsonCommonsFields.description);
		String escapedDescription = escape(description);
		CcpJsonRepresentation jsonWithEscapedDescription = json.put(VisEmailHtmlFields.escapedDescription, escapedDescription);
		return jsonWithEscapedDescription;
	}
}
