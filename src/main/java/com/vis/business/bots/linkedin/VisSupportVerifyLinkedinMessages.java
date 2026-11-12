package com.vis.business.bots.linkedin;

import com.jn.messages.JnSystemMessage;

/**
 * Texts of the {@code verifyLinkedinAddress} command, stored in {@code JnEntitySystemMessage} and seeded by
 * {@code VisEntityResumeMessage} (through {@code VisSupportVerifyLinkedinBotConfiguration}).
 */
public enum VisSupportVerifyLinkedinMessages implements JnSystemMessage {
	/** Shows the report: {@code {recruiterEmail}}, {@code {linkedinAddress}}, {@code {resumeEmailSha1}} and {@code {options}}. */
	ticket,
	/** The options offered to the operator. */
	options,
	/** Answer that is neither of the options: {@code {options}}. */
	notUnderstood,
	/** The address was confirmed as invalid: {@code {linkedinAddress}}. */
	confirmedInvalid,
	/** The address exists and only the recruiter could not open it: {@code {linkedinAddress}} and {@code {recruiterEmail}}. */
	confirmedValid,
	/** There is no resume with the id: {@code {resumeEmailSha1}}. */
	resumeNotFound,
	/** The {@code comment} of the view moved to the twin when the address exists. */
	viewComment,
	;
}
