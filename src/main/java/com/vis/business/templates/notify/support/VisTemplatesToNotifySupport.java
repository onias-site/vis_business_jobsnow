package com.vis.business.templates.notify.support;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.JnEntityJobsnowWarning;
import com.jn.messages.JnSendMessageToUser;

/**
 * Define os templates de notificação enviados ao suporte quando surgem eventos relacionados a skills.
 * Cada constante do enum corresponde a um template de mensagem: new_skill (nova skill sugerida)
 * e new_skill_hierarchy (nova hierarquia de skill). Utiliza JnSendMessageToUser para enviar e-mail
 * e mensagem instantânea ao suporte, bloqueando reenvios duplicados via JnEntityJobsnowWarning.
 */
public enum VisTemplatesToNotifySupport implements CcpBusiness{

	new_skill_hierarchy(),
	new_skill(),
	;


	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String language = json.getAsString(JnEntityEmailTemplateMessage.Fields.language);

		JnSendMessageToUser sender = new JnSendMessageToUser();
		sender
		.addDefaultProcessToEmailSending()
		.and()
		.addDefaultStepToInstantMessageSending()
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
	//FIXME FALTANDO TEMPLATE
	public static class NewSkill extends SendEmail{}
	//FIXME FALTANDO TEMPLATE
	public static class NewSkillHierarchy extends SendEmail{}

	
	
}
