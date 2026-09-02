package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.vis.entities.VisEntitySkillPending;

//FIXME FALTANDO TEMPLATE
public class PendingSkill extends JnBusinessSendMessage{
	protected PendingSkill() {
		super(VisEntitySkillPending.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
}
