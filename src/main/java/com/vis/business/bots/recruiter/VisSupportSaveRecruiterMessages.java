package com.vis.business.bots.recruiter;

import com.jn.messages.JnSystemMessage;

/**
 * Texts of the {@code saveRecruiter} command, stored in {@code JnEntitySystemMessage} and seeded by
 * {@code VisEntityRecruiterSignUpReturn} (through {@code VisSupportSaveRecruiterBotConfiguration}). Every one takes {@code {email}}.
 */
public enum VisSupportSaveRecruiterMessages implements JnSystemMessage {
	/** The e-mail is not a valid address. */
	invalidEmail,
	/** The e-mail is already an active recruiter. */
	alreadyRecruiter,
	/** The inactive recruiter became active again. */
	reactivated,
	/** The new recruiter was saved. */
	saved,
	;
}
