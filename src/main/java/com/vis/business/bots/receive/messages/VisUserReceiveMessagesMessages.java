package com.vis.business.bots.receive.messages;

import com.jn.messages.JnSystemMessage;

/**
 * The texts of {@code /receiveMessages}, seeded by {@link VisUserReceiveMessagesBotConfiguration}. Parameters:
 * {@code {email}} and, in {@code wrongToken}, {@code {remainingAttempts}}.
 */
public enum VisUserReceiveMessagesMessages implements JnSystemMessage {
	/** Asks the token e-mailed to the user. */
	askToken,
	/** The e-mail of the command is invalid. */
	invalidEmail,
	/** The token is wrong: the attempts left and the command to resend the token. */
	wrongToken,
	/** The chat reached the limit of wrong tokens and can no longer use the command. */
	blocked,
	/** The e-mail has no resume: asks the user to register one. */
	resumeNotFound,
	/** The chat was saved in the resume. */
	chatSaved,
	;
}
