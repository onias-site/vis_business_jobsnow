package com.vis.utils;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.entities.JnEntityEmailMessageSent;
import com.jn.messages.JnSendMessageToUser;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnAddDefaultStep;

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
		CcpJsonRepresentation renameField = json
				.renameField(JsonFieldNames.originalEmail, JnJsonCommonsFields.email);
				//TODO GENERALIZAR ESTE PROCESSO

				CcpJsonRepresentation put = renameField
				.put(JnJsonCommonsFields.subjectType, this);
				
			String language = json.getAsObject(JnJsonCommonsFields.language);
			
			JnSendMessageToUser sender = new JnSendMessageToUser();
			JnAddDefaultStep addDefaultProcessToEmailSending = sender
			.addDefaultProcessToEmailSending(JnMessageSenderExceptionHandler.THROWS);
			var soWithAllAddedProcessAnd = addDefaultProcessToEmailSending
			.soWithAllAddedProcessAnd();
			String name = this.name();
			var withTheTemplateEntity = soWithAllAddedProcessAnd
			.withTheTemplateEntity(name);
			var andWithTheEntityToBlockMessageResend = withTheTemplateEntity
			.andWithTheEntityToBlockMessageResend(JnEntityEmailMessageSent.ENTITY);
			var andWithTheMessageValuesFromJson = andWithTheEntityToBlockMessageResend
			.andWithTheMessageValuesFromJson(put);
			var andWithTheSupportLanguage = andWithTheMessageValuesFromJson
			.andWithTheSupportLanguage(language);
			andWithTheSupportLanguage
			.sendAllMessages()
			;

		
		return json;
	}
}
