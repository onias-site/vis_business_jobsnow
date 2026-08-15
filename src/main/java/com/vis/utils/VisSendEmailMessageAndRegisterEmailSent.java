package com.vis.utils;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.entities.JnEntityEmailMessageSent;
import com.jn.messages.JnSendMessageToUser;
import com.jn.json.fields.validation.JnJsonCommonsFields;

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
		//TODO GENERALIZAR ESTE PROCESSO

		CcpJsonRepresentation put = json
				.renameField(JsonFieldNames.originalEmail, JnJsonCommonsFields.email)
				.put(JnJsonCommonsFields.subjectType, this);
				
			String language = json.getAsObject(JnJsonCommonsFields.language);
			
			JnSendMessageToUser sender = new JnSendMessageToUser();
			sender
			.addDefaultProcessToEmailSending(JnMessageSenderExceptionHandler.THROWS)
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
