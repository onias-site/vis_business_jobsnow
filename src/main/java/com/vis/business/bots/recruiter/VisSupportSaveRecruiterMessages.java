package com.vis.business.bots.recruiter;

import com.jn.messages.JnSystemMessage;

/**
 * Texts of the {@code saveRecruiter} command, stored in {@code JnEntitySystemMessage} and seeded by
 * {@code VisEntityRecruiterSignUpReturn} (through {@code VisSupportSaveRecruiterBotConfiguration}). Every one but {@code options} takes {@code {email}}.
 */
public enum VisSupportSaveRecruiterMessages implements JnSystemMessage {
	/** The e-mail is not a valid address. */
	invalidEmail,
	/** The e-mail became an active recruiter before the confirmation of the reactivation. */
	alreadyRecruiter,
	/** The inactive recruiter became active again, with the source {@code support}. */
	reactivated,
	/** The e-mail is an inactive recruiter: does the operator really want to reactivate them? Takes {@code {options}}. */
	confirmReactivation,
	/** The options offered to the operator (no {@code {email}}). */
	options,
	/** Answer that is neither of the options: takes {@code {options}}. */
	notUnderstood,
	/** The operator did not confirm the reactivation. */
	reactivationCancelled,
	/** The e-mail is an active recruiter: does the operator want to inactivate them? Takes {@code {options}}. */
	confirmInactivation,
	/** The recruiter was inactivated and recorded as unsubscribed. */
	inactivated,
	/** The operator did not confirm the inactivation. */
	inactivationCancelled,
	/** The recruiter stopped being active before the confirmation. */
	noLongerActive,
	/** The new recruiter was saved. */
	saved,
	;
}
