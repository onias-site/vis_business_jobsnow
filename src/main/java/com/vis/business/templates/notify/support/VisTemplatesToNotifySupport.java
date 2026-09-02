package com.vis.business.templates.notify.support;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.entities.JnEntityJobsnowWarning;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSendMessageToUser;
import com.jn.messages.JnAddDefaultStep;

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
		String language = json.getAsString(JnJsonCommonsFields.language);
		//TODO GENERALIZAR ESTE PROCESSO
		JnSendMessageToUser sender = new JnSendMessageToUser();
		JnAddDefaultStep addDefaultProcessToEmailSending = sender
		.addDefaultProcessToEmailSending(JnMessageSenderExceptionHandler.THROWS);
		var and = addDefaultProcessToEmailSending
		.and();
		var addDefaultStepToInstantMessageSending = and
		.addDefaultStepToInstantMessageSending(JnMessageSenderExceptionHandler.THROWS);
		var soWithAllAddedProcessAnd = addDefaultStepToInstantMessageSending
		.soWithAllAddedProcessAnd();
		String name = this.name();
		var withTheTemplateEntity = soWithAllAddedProcessAnd
		.withTheTemplateEntity(name);
		var andWithTheEntityToBlockMessageResend = withTheTemplateEntity
		.andWithTheEntityToBlockMessageResend(JnEntityJobsnowWarning.ENTITY);
		var andWithTheMessageValuesFromJson = andWithTheEntityToBlockMessageResend
		.andWithTheMessageValuesFromJson(json);
		var andWithTheSupportLanguage = andWithTheMessageValuesFromJson
		.andWithTheSupportLanguage(language);
		andWithTheSupportLanguage
		.sendAllMessages();
		
		return json;
	}
	
	

	//FIXME FALTANDO TEMPLATE

	//FIXME FALTANDO TEMPLATE


	
	
}
