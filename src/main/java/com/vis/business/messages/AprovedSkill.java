package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.vis.entities.VisEntitySkill;

//FIXME FALTANDO TEMPLATE
public class AprovedSkill extends JnBusinessSendMessage{
	protected AprovedSkill() {
		super(VisEntitySkill.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
}
