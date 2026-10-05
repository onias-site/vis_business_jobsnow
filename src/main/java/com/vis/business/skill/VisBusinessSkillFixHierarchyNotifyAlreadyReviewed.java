package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.messages.JnSendMessageToUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.messages.VisMessages;

/**
 * Handles {@link VisErrorSkillFixHierarchyAlreadyReviewed} for {@link VisEntitySkillFixHierarchyPending}: emails
 * the user that the skills they want to associate with or dissociate from the parent were already handled in
 * earlier requests. The json is the request as it arrived, before the transformer, so {@code email} still
 * holds the readable address.
 */
public class VisBusinessSkillFixHierarchyNotifyAlreadyReviewed implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String topic = VisMessages.VisNotifyUserAboutAlreadyReviewedSkillHierarchy.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.email};
		CcpJsonRepresentation result = new JnSendMessageToUser().sendAllMessages(json, topic, messageTypes, JnMessageSenderExceptionHandler.THROWS);
		return result;
	}
}
