package com.vis.business.job.posting;

import java.util.List;
import com.jn.business.messages.JnSendHtmlInstantMessage;
import java.util.concurrent.Callable;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.instant.messenger.CcpInstantMessenger;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * The job postings of the hour sent to one group of Telegram, in a thread of its own (since 2026-11-26): each piece of
 * each job posting waits {@value #SECONDS_BETWEEN_MESSAGES} seconds before going, so the group never gets messages faster
 * than Telegram allows (429); the pieces after the first of a job posting answer the first one. A job posting that fails
 * is counted and the next one goes on.
 */
public class VisJobPostingsToAGroup implements Callable<CcpJsonRepresentation> {

	/** The counters this class answers. */
	public static enum Fields implements CcpJsonFieldName {
		/** How many messages went. */
		messagesSent,
		/** How many job postings failed in this group. */
		failures,
	}

	/** The pause before each message of a group. */
	static final int SECONDS_BETWEEN_MESSAGES = 3;

	/** The bot that sends. */
	private final CcpJsonFieldName botName;

	/** The token of the bot. */
	private final String botToken;

	/** The chat of the group. */
	private final Long chatId;

	/** The pieces of each job posting, in the order they go. */
	private final List<List<String>> messages;

	/**
	 * Keeps what the thread sends.
	 * @param botName the bot that sends
	 * @param botToken the token of the bot
	 * @param chatId the chat of the group
	 * @param messages the pieces of each job posting
	 */
	public VisJobPostingsToAGroup(CcpJsonFieldName botName, String botToken, Long chatId, List<List<String>> messages) {
		this.botName = botName;
		this.botToken = botToken;
		this.chatId = chatId;
		this.messages = messages;
	}

	/**
	 * Sends every job posting to the group.
	 * @return the counters
	 */
	public CcpJsonRepresentation call() {
		CcpInstantMessenger instantMessenger = CcpDependencyInjection.getDependency(CcpInstantMessenger.class);
		CcpTimeDecorator timer = new CcpTimeDecorator();
		int messagesSent = 0;
		int failures = 0;
		for (List<String> pieces : this.messages) {
			try {
				long replyTo = 0L;
				for (String piece : pieces) {
					timer.sleep(SECONDS_BETWEEN_MESSAGES * 1000);
					CcpJsonRepresentation result = JnSendHtmlInstantMessage.send(instantMessenger, this.botName, this.botToken, this.chatId, replyTo, piece);
					// a messenger that does not answer the id of the message: the next piece answers no message
					boolean answeredTheId = result.containsAllFields(CcpJsonCommonsFields.replyTo);
					replyTo = answeredTheId ? result.getAsLongNumber(CcpJsonCommonsFields.replyTo) : 0L;
					messagesSent++;
				}
			} catch (RuntimeException failure) {
				failure.printStackTrace();
				failures++;
			}
		}
		CcpJsonRepresentation counters = CcpOtherConstants.EMPTY_JSON
				.put(Fields.messagesSent, messagesSent)
				.put(Fields.failures, failures);
		return counters;
	}
}
