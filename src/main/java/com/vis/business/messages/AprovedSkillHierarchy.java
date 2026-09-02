package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;

//FIXME FALTANDO TEMPLATE
public class AprovedSkillHierarchy extends JnBusinessSendMessage{
	protected AprovedSkillHierarchy() {
		super(VisEntitySkillFixHierarchyApproved.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}
}
