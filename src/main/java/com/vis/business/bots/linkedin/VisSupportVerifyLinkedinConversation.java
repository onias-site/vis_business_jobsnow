package com.vis.business.bots.linkedin;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * What the steps of the {@code verifyLinkedinAddress} command share: the language of the session, the replies to the
 * operator and the side (active or inactive) where the resume of the ticket is.
 */
final class VisSupportVerifyLinkedinConversation {

	/** Utility class; not instantiable. */
	private VisSupportVerifyLinkedinConversation() {}

	/**
	 * Returns the language of the session, Portuguese by default.
	 * @param json the session
	 * @return the language
	 */
	static JnLanguage getLanguage(CcpJsonRepresentation json) {
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}

	/**
	 * Puts in the session the text of the item, in the language of the session, as the reply to the operator.
	 * @param json the session
	 * @param message the text
	 * @param parameters the values of the template
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation reply(CcpJsonRepresentation json, VisSupportVerifyLinkedinMessages message, CcpJsonRepresentation parameters) {
		JnLanguage language = getLanguage(json);
		String text = message.getMessage(language, parameters);
		CcpJsonRepresentation jsonWithReply = json.put(JnJsonInstantMessengerFields.botReply, text);
		return jsonWithReply;
	}

	/**
	 * Puts in the session the text of the item filled with the parameters of the command.
	 * @param json the session
	 * @param message the text
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation replyAboutTheTicket(CcpJsonRepresentation json, VisSupportVerifyLinkedinMessages message) {
		CcpJsonRepresentation parameters = json.getJsonPiece(VisJsonCommonsFields.linkedinAddress, VisSupportVerifyLinkedinFields.resumeEmailSha1,
				VisSupportVerifyLinkedinFields.recruiterEmail);
		CcpJsonRepresentation jsonWithReply = reply(json, message, parameters);
		return jsonWithReply;
	}

	/**
	 * Puts in the session the options of the operator in the language of the session.
	 * @param json the session
	 * @param message the text that shows the options
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation replyWithTheOptions(CcpJsonRepresentation json, VisSupportVerifyLinkedinMessages message) {
		JnLanguage language = getLanguage(json);
		String options = VisSupportVerifyLinkedinMessages.options.getMessage(language);
		CcpJsonRepresentation ticketParameters = json.getJsonPiece(VisJsonCommonsFields.linkedinAddress, VisSupportVerifyLinkedinFields.resumeEmailSha1,
				VisSupportVerifyLinkedinFields.recruiterEmail);
		CcpJsonRepresentation parameters = ticketParameters.put(VisSupportVerifyLinkedinFields.options, options);
		CcpJsonRepresentation jsonWithReply = reply(json, message, parameters);
		return jsonWithReply;
	}

	/**
	 * The entity where the resume of the ticket is: {@code vis_resume} when active, {@code vis_inactive_resume} when
	 * inactive. The command carries the id of the document, not its primary key, so it is read by the id, through
	 * {@link CcpCrud}.
	 * @param resumeId the id of the resume
	 * @return the entity, or an empty json when the resume is in neither
	 */
	static CcpJsonRepresentation getSideOfTheResume(String resumeId) {
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		String activeName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		boolean active = crud.exists(activeName, resumeId);
		if(active) {
			CcpJsonRepresentation activeSide = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.entity, activeName);
			return activeSide;
		}
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		String inactiveName = twinEntity.getEntityMetaData().entityName;
		boolean inactive = crud.exists(inactiveName, resumeId);
		if(inactive) {
			CcpJsonRepresentation inactiveSide = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.entity, inactiveName);
			return inactiveSide;
		}
		return CcpOtherConstants.EMPTY_JSON;
	}
}
