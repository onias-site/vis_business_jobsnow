package com.vis.business.bots.job.posting;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;

/** The replies of the {@code saveJobPosting} command to the operator, in the language of the session. */
final class VisSupportSaveJobPostingConversation {

	/** Utility class; not instantiable. */
	private VisSupportSaveJobPostingConversation() {}

	/** Parameters of the texts. */
	enum Fields implements CcpJsonFieldName {
		/** How many characters the text typed has. */
		length,
	}

	/**
	 * Puts in the session the text of the item, in the language of the session (Portuguese by default), filled with the
	 * parameters, as the reply to the operator.
	 * @param session the session
	 * @param message the text
	 * @param parameters the values of the text
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation reply(CcpJsonRepresentation session, VisSupportSaveJobPostingMessages message, CcpJsonRepresentation parameters) {
		String languageName = session.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		String text = message.getMessage(language, parameters);
		CcpJsonRepresentation sessionWithReply = session.put(JnJsonInstantMessengerFields.botReply, text);
		return sessionWithReply;
	}
}
