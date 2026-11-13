package com.vis.business.bots.receive.messages;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;
import com.vis.entities.VisEntityLoginTokenAttempts;

/**
 * What the steps of {@code /receiveMessages} share: the replies to the user, in the language of the session, and the
 * wrong tokens of the chat.
 */
final class VisUserReceiveMessagesConversation {

	/** Utility class; not instantiable. */
	private VisUserReceiveMessagesConversation() {}

	/** The parameters of the texts. */
	enum Fields implements CcpJsonFieldName {
		/** The wrong tokens the chat may still type. */
		remainingAttempts,
	}

	/**
	 * Puts in the session the text, in the language of the session (Portuguese when it has none), as the reply.
	 * @param json the session, with {@code email}
	 * @param message the text
	 * @param moreParameters the values of the text besides {@code email}
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation reply(CcpJsonRepresentation json, VisUserReceiveMessagesMessages message, CcpJsonRepresentation moreParameters) {
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		CcpJsonRepresentation emailParameter = json.getJsonPiece(JnJsonCommonsFields.email);
		CcpJsonRepresentation parameters = emailParameter.mergeWithAnotherJson(moreParameters);
		String text = message.getMessage(language, parameters);
		CcpJsonRepresentation jsonWithReply = json.put(JnJsonInstantMessengerFields.botReply, text);
		return jsonWithReply;
	}

	/**
	 * Puts in the session the text with only {@code email} as the reply.
	 * @param json the session
	 * @param message the text
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation reply(CcpJsonRepresentation json, VisUserReceiveMessagesMessages message) {
		CcpJsonRepresentation jsonWithReply = reply(json, message, CcpOtherConstants.EMPTY_JSON);
		return jsonWithReply;
	}

	/**
	 * The key of the wrong tokens of the chat of the session.
	 * @param json the session, with {@code chatId}
	 * @return the key
	 */
	static CcpJsonRepresentation getAttemptsKey(CcpJsonRepresentation json) {
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		CcpJsonRepresentation attemptsKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityLoginTokenAttempts.Fields.chatId, chatId);
		return attemptsKey;
	}

	/**
	 * The wrong tokens typed by the chat of the session.
	 * @param json the session, with {@code chatId}
	 * @return the count, 0 when the chat typed none
	 */
	static int getAttempts(CcpJsonRepresentation json) {
		CcpJsonRepresentation attemptsKey = getAttemptsKey(json);
		try {
			CcpJsonRepresentation attempts = VisEntityLoginTokenAttempts.ENTITY.getOneById(attemptsKey);
			Integer count = attempts.getAsIntegerNumber(VisEntityLoginTokenAttempts.Fields.attempts);
			return count;
		} catch (CcpErrorFlowDisturb noWrongToken) {
			return 0;
		}
	}

	/**
	 * The limit of wrong tokens, the same of the login screen.
	 * @return {@code JnSystemProperties.maxAttempts()}
	 */
	static int getMaxAttempts() {
		int maxAttempts = JnSystemProperties.INSTANCE.maxAttempts();
		return maxAttempts;
	}
}
