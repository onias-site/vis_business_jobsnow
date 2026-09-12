package com.vis.business.skills.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityUserRequest;

//FIXME FALTANDO TEMPLATE
public class VisNotifyUserAboutRejectedSkill extends JnBusinessSendMessage{
	protected VisNotifyUserAboutRejectedSkill() {
		super(JnEntityUserRequest.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
	
	public JnMessageType[] getMessageTypes() {
		return new JnMessageType[] {JnMessageType.email};
	}
}
