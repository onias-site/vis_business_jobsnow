package com.vis.business.skills.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;

// FIXME template faltando
public class VisNotifySupportandUserAboutNewSkillPendingRequest extends JnBusinessSendMessage{

	protected VisNotifySupportandUserAboutNewSkillPendingRequest() {
		super(JnMessageSenderExceptionHandler.THROWS);
	}

	public JnMessageType[] getMessageTypes() {
		return new JnMessageType[] {JnMessageType.email, JnMessageType.instantMessenger};
	}
}
