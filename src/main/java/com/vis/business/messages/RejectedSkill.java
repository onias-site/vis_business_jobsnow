package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.vis.entities.VisEntitySkillRejected;

//FIXME FALTANDO TEMPLATE
public class RejectedSkill extends JnBusinessSendMessage{
	protected RejectedSkill() {
		super(VisEntitySkillRejected.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
}
