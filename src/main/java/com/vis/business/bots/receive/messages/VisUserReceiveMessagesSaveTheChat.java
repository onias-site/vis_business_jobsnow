package com.vis.business.bots.receive.messages;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.entities.JnEntityInstantMessengerUser;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.entities.VisEntityResume;

/**
 * Saves the chat of the session as the chat of the e-mail in {@code jn_instant_messenger_user} (since 2026-11-13; until
 * then the chat went to the resume), once the e-mail has a resume, active ({@code vis_resume}) or inactive
 * ({@code vis_inactive_resume}), read on both sides at once. A chat answers for one e-mail only: the records of the chat
 * under another e-mail (the user is changing the e-mail) are deleted before the new one is saved. An e-mail without
 * resume is told to register one ({@code resumeNotFound}).
 * <p>
 * The search by chat is not by id: the command runs in the consumer of its queue, not in an online request (OQL).
 */
public class VisUserReceiveMessagesSaveTheChat implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisUserReceiveMessagesSaveTheChat INSTANCE = new VisUserReceiveMessagesSaveTheChat();

	private VisUserReceiveMessagesSaveTheChat() {}

	/**
	 * Saves the chat of the e-mail.
	 * @param json the session, with {@code email} and {@code chatId}
	 * @return the session, as it came
	 * @throws CcpErrorFlowDisturb with {@code resumeNotFound}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, email);
		CcpJsonRepresentation bothSides = VisEntityResume.ENTITY.getOneByIdAnyWhere(resumeKey);
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		CcpJsonRepresentation activeResume = bothSides.getInnerJson(VisEntityResume.ENTITY);
		CcpJsonRepresentation inactiveResume = bothSides.getInnerJson(twinEntity);
		boolean resumeNotFound = activeResume.isEmpty() && inactiveResume.isEmpty();
		if(resumeNotFound) {
			CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.resumeNotFound);
			return VisUserReceiveMessagesStatus.resumeNotFound.throwException(jsonWithReply);
		}
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		deleteThePreviousEmailsOfTheChat(chatId);
		CcpJsonRepresentation instantMessengerUser = CcpOtherConstants.EMPTY_JSON
				.put(JnEntityInstantMessengerUser.Fields.email, email)
				.put(JnEntityInstantMessengerUser.Fields.chatId, chatId);
		JnEntityInstantMessengerUser.ENTITY.save(instantMessengerUser);
		return json;
	}

	/**
	 * Deletes every record of the chat, whatever the e-mail. The first step already ended the command when the e-mail
	 * had this chat, so what is found here belongs to another e-mail.
	 * @param chatId the chat of the session
	 */
	private static void deleteThePreviousEmailsOfTheChat(Long chatId) {
		CcpQueryOptions recordsOfTheChat = CcpQueryOptions.INSTANCE
				.startQuery()
				.startBool()
				.startMust()
				.term(JnEntityInstantMessengerUser.Fields.chatId, chatId)
				.endMustAndBackToBool()
				.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		String emailField = JnEntityInstantMessengerUser.Fields.email.getValue();
		List<CcpJsonRepresentation> previousRecords = recordsOfTheChat
				.selectFrom(JnEntityInstantMessengerUser.ENTITY)
				.getResultAsList(emailField);
		// the stored e-mails are already hashed: without the fields transformer, the id is calculated from them as they are
		CcpEntity instantMessengerUsers = CcpEntityFactory.getCustomEntity(JnEntityInstantMessengerUser.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		for (CcpJsonRepresentation previousRecord : previousRecords) {
			CcpJsonRepresentation previousKey = previousRecord.getJsonPiece(JnEntityInstantMessengerUser.Fields.email);
			instantMessengerUsers.delete(previousKey);
		}
	}
}
