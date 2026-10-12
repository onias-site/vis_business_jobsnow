package com.vis.messages;

import com.ccp.decorators.CcpJsonFieldName;

/** Placeholders of the vis email templates filled with text escaped by {@code VisEmailHtml}. */
public enum VisEmailHtmlFields implements CcpJsonFieldName{
	/** The candidate's justification ({@code description}), escaped for the HTML body of the email. */
	escapedDescription,
}
