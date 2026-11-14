package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.entities.JnEntityInstantMessengerUser;
import com.vis.entities.VisEntityResumeMessage;
import com.vis.json.fields.validation.VisResumeMessageTypes;

/**
 * Action of {@link VisResumeOwnerChats}, run for each owner whose e-mail has a chat in {@code jn_instant_messenger_user}
 * (since 2026-11-13): saves the message in {@code vis_resume_message}, whose save sends it through the user bot.
 */
class VisBusinessSaveTheMessageToTheOwner implements CcpBusiness {

	/** The entity of the chats, as read by the procedure (keyed by the hashed e-mail). */
	private final CcpEntity instantMessengerUsers;

	/** The kind of the message. */
	private final VisResumeMessageTypes messageType;

	/** The text of the message, already in the language of the owner. */
	private final String message;

	/**
	 * Keeps what the message needs.
	 * @param instantMessengerUsers the entity of the chats, as read by the procedure
	 * @param messageType the kind of the message
	 * @param message the text of the message
	 */
	VisBusinessSaveTheMessageToTheOwner(CcpEntity instantMessengerUsers, VisResumeMessageTypes messageType, String message) {
		this.instantMessengerUsers = instantMessengerUsers;
		this.messageType = messageType;
		this.message = message;
	}

	/**
	 * Saves the message to the chat of the owner.
	 * @param ownerWithTheStoredChat the key of the owner, holding the stored record of the chat under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation ownerWithTheStoredChat) {
		CcpJsonRepresentation storedChat = ownerWithTheStoredChat.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, this.instantMessengerUsers);
		// the stored number comes back from the JSON as a Double
		Long chatId = storedChat.getAsLongNumber(JnEntityInstantMessengerUser.Fields.chatId);
		CcpJsonRepresentation resumeMessage = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityResumeMessage.Fields.chatId, chatId)
				.put(VisEntityResumeMessage.Fields.messageType, this.messageType)
				.put(VisEntityResumeMessage.Fields.message, this.message);
		VisEntityResumeMessage.ENTITY.save(resumeMessage);
		return ownerWithTheStoredChat;
	}
}
