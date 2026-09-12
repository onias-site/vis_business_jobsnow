package com.vis.business.skills.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;

//FIXME FALTANDO TEMPLATE
public class VisNotifyUserAboutAprovedSkillHierarchy extends JnBusinessSendMessage{
	protected VisNotifyUserAboutAprovedSkillHierarchy() {
		super(VisEntitySkillFixHierarchyApproved.ENTITY, JnMessageSenderExceptionHandler.THROWS);
	}

	public JnMessageType[] getMessageTypes() {
		return new JnMessageType[] {JnMessageType.email};
	}
}
