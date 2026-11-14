package com.vis.business.bots.receive.messages;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.business.session.JnBusinessCheckLoginToken;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityLoginTokenAttempts;

/**
 * Second step of {@code /receiveMessages <email>}: the token typed by the user, checked by the cost center of the login
 * through {@link JnBusinessCheckLoginToken}. A right token clears the wrong ones of the chat and saves the chat of the
 * e-mail in {@code jn_instant_messenger_user} ({@link VisUserReceiveMessagesSaveTheChat}). A wrong one is counted in
 * {@code vis_login_token_attempts} (1 at the first one): the user is told the attempts left and the command
 * {@code /resendLoginToken}, and the step waits for another token ({@code wrongToken}); at the limit, the chat is blocked
 * ({@code blocked}).
 */
public class VisUserReceiveMessagesCheckToken implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisUserReceiveMessagesCheckToken INSTANCE = new VisUserReceiveMessagesCheckToken();

	private VisUserReceiveMessagesCheckToken() {}

	/**
	 * Checks the token.
	 * @param json the session, with {@code email}, {@code chatId} and the typed token
	 * @return the session with the reply of the chat saved
	 * @throws CcpErrorFlowDisturb with {@code wrongToken}, {@code blocked} or {@code resumeNotFound}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonCommonsFields.email);
		String typedToken = json.getAsString(JnJsonCommonsFields.typedValue);
		CcpJsonRepresentation tokenToCheck = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(JnJsonCommonsFields.token, typedToken);
		CcpJsonRepresentation checked = JnBusinessCheckLoginToken.INSTANCE.execute(tokenToCheck);
		boolean wrongToken = false == checked.getAsBoolean(JnBusinessCheckLoginToken.Fields.rightToken);
		if(wrongToken) {
			return countTheWrongToken(json);
		}
		CcpJsonRepresentation attemptsKey = VisUserReceiveMessagesConversation.getAttemptsKey(json);
		VisEntityLoginTokenAttempts.ENTITY.delete(attemptsKey);
		VisUserReceiveMessagesSaveTheChat.INSTANCE.execute(json);
		CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.chatSaved);
		return jsonWithReply;
	}

	/**
	 * Counts one more wrong token and tells the user.
	 * @param json the session
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code wrongToken}, or {@code blocked} at the limit
	 */
	private static CcpJsonRepresentation countTheWrongToken(CcpJsonRepresentation json) {
		int previousAttempts = VisUserReceiveMessagesConversation.getAttempts(json);
		int attempts = previousAttempts + 1;
		CcpJsonRepresentation attemptsKey = VisUserReceiveMessagesConversation.getAttemptsKey(json);
		CcpJsonRepresentation attemptsRecord = attemptsKey.put(VisEntityLoginTokenAttempts.Fields.attempts, attempts);
		VisEntityLoginTokenAttempts.ENTITY.save(attemptsRecord);
		int maxAttempts = VisUserReceiveMessagesConversation.getMaxAttempts();
		boolean blocked = attempts >= maxAttempts;
		if(blocked) {
			CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.blocked);
			return VisUserReceiveMessagesStatus.blocked.throwException(jsonWithReply);
		}
		int remainingAttempts = maxAttempts - attempts;
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(VisUserReceiveMessagesConversation.Fields.remainingAttempts, remainingAttempts);
		CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.wrongToken, parameters);
		return VisUserReceiveMessagesStatus.wrongToken.throwException(jsonWithReply);
	}
}
