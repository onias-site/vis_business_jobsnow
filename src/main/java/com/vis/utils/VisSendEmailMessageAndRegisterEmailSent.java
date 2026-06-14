package com.vis.utils;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.business.CcpBusiness;
import com.jn.entities.JnEntityEmailMessageSent;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.messages.JnSendMessageToUser;
/**
 * Define os templates de e-mail de notificação ao candidato sobre o status do salvamento do currículo,
 * enviando a mensagem via JnSendMessageToUser e bloqueando reenvios duplicados via JnEntityEmailMessageSent.
 */
public enum VisSendEmailMessageAndRegisterEmailSent implements CcpBusiness, CcpJsonFieldName{
	
	resumeSuccessSaving,
	resumeErrorSaving
;
	enum JsonFieldNames implements CcpJsonFieldName{
		originalEmail
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation put = json
				.renameField(JsonFieldNames.originalEmail, JnEntityEmailMessageSent.Fields.email)
				.put(JnEntityEmailMessageSent.Fields.subjectType, this);
				
			String language = json.getAsObject(JnEntityEmailTemplateMessage.Fields.language);
			
			JnSendMessageToUser sender = new JnSendMessageToUser();
			sender
			.addDefaultProcessToEmailSending()
			.soWithAllAddedProcessAnd()
			.withTheTemplateEntity(this.name())
			.andWithTheEntityToBlockMessageResend(JnEntityEmailMessageSent.ENTITY)
			.andWithTheMessageValuesFromJson(put)
			.andWithTheSupportLanguage(language)
			.sendAllMessages()
			;

		
		return json;
	}
}
