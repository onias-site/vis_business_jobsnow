package com.vis.business.bots;

/**
 * The commands that vis adds to the user bot (since 2026-11-12). vis seeds their configuration in the bot entities of
 * jn and the bot runs them by the class names found there, without knowing vis.
 */
public enum VisUserBotCommands {
	/**
	 * {@code /receiveMessages <email>}: after the login token of the e-mail, the chat goes to the resume of the e-mail,
	 * and the messages about the resume reach the user here.
	 */
	receiveMessages,
	;
}
