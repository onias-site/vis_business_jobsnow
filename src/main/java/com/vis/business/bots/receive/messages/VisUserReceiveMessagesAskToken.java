package com.vis.business.bots.receive.messages;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * First step of {@code /receiveMessages <email>} in the user bot (since 2026-11-12): asks the login token e-mailed to
 * the user. A chat that reached the limit of wrong tokens is told it is blocked ({@code blocked}), and an invalid e-mail
 * ends the command ({@code invalidEmail}).
 */
public class VisUserReceiveMessagesAskToken implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisUserReceiveMessagesAskToken INSTANCE = new VisUserReceiveMessagesAskToken();

	private VisUserReceiveMessagesAskToken() {}

	/**
	 * Asks the token.
	 * @param json the session, with {@code email} and {@code chatId}
	 * @return the session with the request of the token
	 * @throws CcpErrorFlowDisturb with {@code blocked} or {@code invalidEmail}
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
		CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.askToken);
		return jsonWithReply;
	}
}
