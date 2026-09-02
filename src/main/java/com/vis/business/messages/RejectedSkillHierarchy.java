package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.vis.entities.VisEntitySkillFixHierarchyRejected;

//FIXME FALTANDO TEMPLATE
public class RejectedSkillHierarchy extends JnBusinessSendMessage{
	protected RejectedSkillHierarchy() {
		super(VisEntitySkillFixHierarchyRejected.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
}
