package com.vis.business.templates.email;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.messages.JnSendMessageToUser;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillFixHierarchyRejected;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;

public enum VisEmailTemplates implements CcpBusiness{

	reproved_skill_hierarchy(VisEntitySkillFixHierarchyPending.ENTITY),
	aproved_skill_hierarchy(VisEntitySkillFixHierarchyApproved.ENTITY),
	peding_skill_hierarchy(VisEntitySkillFixHierarchyRejected.ENTITY),
	rejected_skill(VisEntitySkillRejected.ENTITY),
	pending_skill(VisEntitySkillPending.ENTITY),
	aproved_skill(VisEntitySkill.ENTITY),
	;

	private final CcpEntity entity;
	
	private VisEmailTemplates(CcpEntity entity) {
		this.entity = entity;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String language = json.getAsString(JnEntityEmailTemplateMessage.Fields.language);

		JnSendMessageToUser getMessage = new JnSendMessageToUser();

		getMessage
		.addDefaultProcessForEmailSending()
		.soWithAllAddedProcessAnd()
		.withTheTemplateEntity(this.name())
		.andWithTheEntityToBlockMessageResend(this.entity)
		.andWithTheMessageValuesFromJson(json)
		.andWithTheSupportLanguage(language)
		.sendAllMessages()
		;
		
		return json;
	}
	
	
	private static class SendEmail implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String simpleName = this.getClass().getSimpleName();
			String lowerCase = new CcpStringDecorator(simpleName).text().toSnakeCase().content.toLowerCase();
			VisEmailTemplates valueOf = VisEmailTemplates.valueOf(lowerCase);
			CcpJsonRepresentation apply = valueOf.apply(json);
			return apply;
		}
		
	}
	
	public static class RejectedSkillHierarchy extends SendEmail{}
	public static class AprovedSkillHierarchy extends SendEmail{}
	public static class PedingSkillHierarchy extends SendEmail{}
	public static class RejectedSkill extends SendEmail{}
	public static class PendingSkill extends SendEmail{}
	public static class AprovedSkill extends SendEmail{}
	
	
}
