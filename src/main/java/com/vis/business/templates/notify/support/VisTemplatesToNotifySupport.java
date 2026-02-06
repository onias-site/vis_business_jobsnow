package com.vis.business.templates.notify.support;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.JnEntityJobsnowWarning;
import com.jn.messages.JnSendMessageToUser;

public enum VisTemplatesToNotifySupport implements CcpBusiness{

	new_skill_hierarchy(),
	new_skill(),
	;


	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String language = json.getAsString(JnEntityEmailTemplateMessage.Fields.language);

		JnSendMessageToUser sender = new JnSendMessageToUser();
		sender
		.addDefaultProcessForEmailSending()
		.and()
		.addDefaultStepForTelegramSending()
		.soWithAllAddedProcessAnd()
		.withTheTemplateEntity(this.name())
		.andWithTheEntityToBlockMessageResend(JnEntityJobsnowWarning.ENTITY)
		.andWithTheMessageValuesFromJson(json)
		.andWithTheSupportLanguage(language)
		.sendAllMessages();
		
		return json;
	}
	
	
	private static class SendEmail implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String simpleName = this.getClass().getSimpleName();
			String lowerCase = new CcpStringDecorator(simpleName).text().toSnakeCase().content.toLowerCase();
			VisTemplatesToNotifySupport valueOf = VisTemplatesToNotifySupport.valueOf(lowerCase);
			CcpJsonRepresentation apply = valueOf.apply(json);
			return apply;
		}
		
	}
	
	public static class NewSkill extends SendEmail{}
	public static class NewSkillHierarchy extends SendEmail{}

	
	
}
