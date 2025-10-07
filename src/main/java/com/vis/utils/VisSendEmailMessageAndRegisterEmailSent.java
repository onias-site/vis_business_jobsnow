package com.vis.utils;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.jn.entities.JnEntityEmailMessageSent;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.messages.JnSendMessage;
public enum VisSendEmailMessageAndRegisterEmailSent implements CcpTopic, CcpJsonFieldName{
//TODO JSON VALIDATIONS	
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
			
			JnSendMessage sender = new JnSendMessage();
			sender
			.addDefaultProcessForEmailSending()
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
