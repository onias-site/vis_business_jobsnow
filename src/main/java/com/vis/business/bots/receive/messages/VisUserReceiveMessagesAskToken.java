package com.vis.business.bots.receive.messages;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.entities.JnEntityInstantMessengerUser;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * First step of {@code /receiveMessages <email>} in the user bot (since 2026-11-12): asks the login token e-mailed to
 * the user. A chat that reached the limit of wrong tokens is told it is blocked ({@code blocked}), an invalid e-mail
 * ends the command ({@code invalidEmail}), and so does an e-mail already registered with this chat in
 * {@code jn_instant_messenger_user} ({@code alreadyRegistered}, since 2026-11-13).
 */
public class VisUserReceiveMessagesAskToken implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisUserReceiveMessagesAskToken INSTANCE = new VisUserReceiveMessagesAskToken();

	private VisUserReceiveMessagesAskToken() {}

	/**
	 * Asks the token.
	 * @param json the session, with {@code email} and {@code chatId}
	 * @return the session with the request of the token
	 * @throws CcpErrorFlowDisturb with {@code blocked}, {@code invalidEmail} or {@code alreadyRegistered}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		int attempts = VisUserReceiveMessagesConversation.getAttempts(json);
		int maxAttempts = VisUserReceiveMessagesConversation.getMaxAttempts();
		boolean blocked = attempts >= maxAttempts;
		if(blocked) {
			CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.blocked);
			return VisUserReceiveMessagesStatus.blocked.throwException(jsonWithReply);
		}
		String email = json.getAsString(JnJsonCommonsFields.email);
		CcpStringDecorator emailText = new CcpStringDecorator(email);
		CcpEmailDecorator emailDecorator = emailText.email();
		boolean invalidEmail = false == emailDecorator.isValid();
		if(invalidEmail) {
			CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.invalidEmail);
			return VisUserReceiveMessagesStatus.invalidEmail.throwException(jsonWithReply);
		}
		boolean alreadyRegistered = isAlreadyRegistered(json);
		if(alreadyRegistered) {
			CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.alreadyRegistered);
			return VisUserReceiveMessagesStatus.alreadyRegistered.throwException(jsonWithReply);
		}
		CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.askToken);
		return jsonWithReply;
	}

	/**
	 * Whether {@code jn_instant_messenger_user} already holds the e-mail with the chat of the session (since 2026-11-13).
	 * The e-mail under another chat is not a repetition: the right token moves it to this chat.
	 * @param json the session, with {@code email} and {@code chatId}
	 * @return {@code true} when the record would be saved again as it is
	 */
	private static boolean isAlreadyRegistered(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation instantMessengerUserKey = CcpOtherConstants.EMPTY_JSON.put(JnEntityInstantMessengerUser.Fields.email, email);
		CcpJsonRepresentation storedRecord;
		try {
			storedRecord = JnEntityInstantMessengerUser.ENTITY.getOneById(instantMessengerUserKey);
		} catch (CcpErrorFlowDisturb notRegistered) {
			return false;
		}
		// the stored number comes back from the JSON as a Double
		Long storedChatId = storedRecord.getAsLongNumber(JnEntityInstantMessengerUser.Fields.chatId);
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		boolean sameChat = chatId.equals(storedChatId);
		return sameChat;
	}
}
