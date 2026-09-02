package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

//FIXME FALTANDO TEMPLATE
public class PendingSkillHierarchy extends JnBusinessSendMessage{
	protected PendingSkillHierarchy() {
		super(VisEntitySkillFixHierarchyPending.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
}
