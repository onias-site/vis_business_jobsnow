package com.vis.business.bots.recruiter;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

/** The replies of the {@code saveRecruiter} command to the operator, in the language of the session. */
final class VisSupportSaveRecruiterConversation {

	/** Utility class; not instantiable. */
	private VisSupportSaveRecruiterConversation() {}

	/** Parameters of the texts. */
	enum Fields implements CcpJsonFieldName {
		/** The options of the confirmation of the reactivation. */
		options,
	}

	/**
	 * Puts in the session the text of the item, in the language of the session (Portuguese by default), filled with the
	 * e-mail, as the reply to the operator.
	 * @param session the session
	 * @param message the text
	 * @param email the e-mail shown in the text
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation reply(CcpJsonRepresentation session, VisSupportSaveRecruiterMessages message, String email) {
		JnLanguage language = getLanguage(session);
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.email, email);
		String text = message.getMessage(language, parameters);
		CcpJsonRepresentation sessionWithReply = session.put(JnJsonInstantMessengerFields.botReply, text);
		return sessionWithReply;
	}

	/**
	 * Puts in the session the text of the item, filled with the e-mail and with the options of the confirmation of the
	 * reactivation ({@link VisSupportSaveRecruiterMessages#options}), as the reply to the operator.
	 * @param session the session
	 * @param message the text that shows the options
	 * @param email the e-mail shown in the text
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation replyWithTheOptions(CcpJsonRepresentation session, VisSupportSaveRecruiterMessages message, String email) {
		JnLanguage language = getLanguage(session);
		String options = VisSupportSaveRecruiterMessages.options.getMessage(language);
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(Fields.options, options);
		String text = message.getMessage(language, parameters);
		CcpJsonRepresentation sessionWithReply = session.put(JnJsonInstantMessengerFields.botReply, text);
		return sessionWithReply;
	}

	/**
	 * Returns the language of the session, Portuguese by default.
	 * @param session the session
	 * @return the language
	 */
	private static JnLanguage getLanguage(CcpJsonRepresentation session) {
		String languageName = session.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}
}
