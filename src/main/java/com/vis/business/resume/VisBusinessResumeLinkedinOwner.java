package com.vis.business.resume;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSendMessageToUser;
import com.vis.entities.VisEntityResume;
import com.vis.messages.VisMessages;

/**
 * Runs after each save of a resume: adds its e-mail to the LinkedIn address in {@link VisEntityResumeLinkedin}. An
 * e-mail already in the address changes nothing. When the address then has more than one e-mail, the support bot
 * operator is told ({@code VisNotifySupportAboutSharedLinkedin}). The address is read by id, which the online save is
 * allowed to do.
 */
public class VisBusinessResumeLinkedinOwner implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessResumeLinkedinOwner INSTANCE = new VisBusinessResumeLinkedinOwner();

	private VisBusinessResumeLinkedinOwner() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the saved resume (with the e-mail already hashed and the original kept in {@code originalEmail})
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String linkedinAddress = json.getAsString(VisEntityResume.Fields.linkedinAddress);
		CcpJsonRepresentation addressKey = VisResumeLinkedinEmails.getAddressKey(linkedinAddress);

		CcpJsonRepresentation resumeWithHashedEmail = VisResumeEmails.withHashedEmail(json);
		String hashedEmail = resumeWithHashedEmail.getAsString(JnJsonCommonsFields.email);

		List<String> emails = VisResumeLinkedinEmails.getEmails(addressKey);
		boolean alreadyInTheAddress = emails.contains(hashedEmail);

		if (alreadyInTheAddress) {
			return json;
		}

		VisResumeLinkedinEmails.addEmail(addressKey, hashedEmail);

		// read again after the atomic write: another resume may have added its e-mail at the same moment, and the read
		// before the write would not show it
		List<String> emailsAfterAdding = VisResumeLinkedinEmails.getEmails(addressKey);
		boolean usedByThisEmailOnly = emailsAfterAdding.size() <= 1;

		if (usedByThisEmailOnly) {
			return json;
		}

		String readableEmail = resumeWithHashedEmail.getAsString(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonRepresentation notice = addressKey.put(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail, readableEmail);
		String topic = VisMessages.VisNotifySupportAboutSharedLinkedin.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.instantMessenger};
		new JnSendMessageToUser().sendAllMessages(notice, topic, messageTypes, JnMessageSenderExceptionHandler.LOG);
		return json;
	}
}
