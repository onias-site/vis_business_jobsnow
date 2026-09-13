package com.vis.business.skills.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;

//FIXME FALTANDO TEMPLATE
public class VisNotifySupportAndUserAboutPendingSkillHierarchyRequest extends JnBusinessSendMessage {
	protected VisNotifySupportAndUserAboutPendingSkillHierarchyRequest() {
		super(JnMessageSenderExceptionHandler.THROWS);
	}

	public JnMessageType[] getMessageTypes() {
		return new JnMessageType[] { JnMessageType.instantMessenger, JnMessageType.email };
	}
}
