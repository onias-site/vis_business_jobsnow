package com.vis.business.bots.receive.messages;

import com.ccp.process.CcpProcessStatus;

/**
 * Diversions of the steps of {@code /receiveMessages}, mapped in the {@code stepFlow} of each step: the token was wrong
 * ({@code wrongToken}, the same step waits for another one); the e-mail is invalid, the chat is blocked or the e-mail has
 * no resume (the session ends).
 */
public enum VisUserReceiveMessagesStatus implements CcpProcessStatus {
	/** Status 400: the e-mail of the command is invalid. */
	invalidEmail(400),
	/** Status 401: the token is wrong, and the chat still has attempts. */
	wrongToken(401),
	/** Status 403: the chat reached the limit of wrong tokens. */
	blocked(403),
	/** Status 404: the e-mail has no resume, active or inactive. */
	resumeNotFound(404),
	/** Status 409: the e-mail is already registered with this chat in {@code jn_instant_messenger_user} (since 2026-11-13). */
	alreadyRegistered(409),
	;

	/** The HTTP status code. */
	public final int status;

	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisUserReceiveMessagesStatus(int status) {
		this.status = status;
	}

	/**
	 * Returns the HTTP status code.
	 * @return the status code
	 */
	public int asNumber() {
		return this.status;
	}
}
