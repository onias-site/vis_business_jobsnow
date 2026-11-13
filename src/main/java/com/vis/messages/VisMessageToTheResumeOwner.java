package com.vis.messages;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityMessageSenderExceptionHandler;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityMessageType;
import com.jn.business.messages.JnMessageTemplate;
import com.jn.business.messages.JnMessageType;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.messages.JnRepeatableMessage;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityResumeMessage;
import com.vis.json.fields.validation.VisResumeMessageTypes;

/**
 * A message to the owner of a resume through the user bot (since 2026-11-12), sent by the save of
 * {@code vis_resume_message}, whose {@code message} already holds the text and prevails over the template. The entity
 * names every message class in its annotation and each save runs all of them: each class sends only the records of its
 * own {@code messageType}. Repeatable: the same kind of message may reach the owner again.
 */
public abstract class VisMessageToTheResumeOwner implements JnMessageTemplate, JnRepeatableMessage {

	private final VisResumeMessageTypes messageType;

	/**
	 * @param messageType the kind of record this class sends
	 */
	protected VisMessageToTheResumeOwner(VisResumeMessageTypes messageType) {
		this.messageType = messageType;
	}

	/**
	 * Sends the record only when it is of the kind of this class.
	 * @param json the record of {@code vis_resume_message}
	 * @param exceptionHandler what happens when the sending fails
	 * @param messageTypes the kinds of message sent
	 * @return the JSON after the sending, or the record as it came when it is of another kind
	 */
	public CcpJsonRepresentation sendMessage(CcpJsonRepresentation json, CcpEntityMessageSenderExceptionHandler exceptionHandler, CcpEntityMessageType... messageTypes) {
		String typeOfTheRecord = json.getAsString(VisEntityResumeMessage.Fields.messageType);
		String typeOfThisClass = this.messageType.name();
		boolean anotherKind = false == typeOfThisClass.equals(typeOfTheRecord);
		if(anotherKind) {
			return json;
		}
		CcpJsonRepresentation result = JnMessageTemplate.super.sendMessage(json, exceptionHandler, messageTypes);
		return result;
	}

	/**
	 * Puts the user bot, which sends to the owner (the parameters of the sending are found by bot and template, and a
	 * message without bot goes through the support one), and the language of the template, Portuguese while the resumes
	 * keep no language.
	 * @param json the record of {@code vis_resume_message}
	 * @return the values of the template
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String userBot = JnMessageType.JnBotType.user.name();
		CcpJsonRepresentation jsonWithBot = json.put(JnJsonInstantMessengerFields.botName, userBot);
		boolean languageInformed = json.containsField(JnJsonCommonsFields.language);
		if(languageInformed) {
			return jsonWithBot;
		}
		CcpJsonRepresentation jsonWithLanguage = jsonWithBot.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		return jsonWithLanguage;
	}
}
