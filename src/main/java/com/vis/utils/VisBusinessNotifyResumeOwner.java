package com.vis.utils;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.JnEntityEmailMessageSent;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.messages.JnSendMessage;

public class VisBusinessNotifyResumeOwner implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{

	public final CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String subjectType = this.getClass().getName();
		CcpJsonRepresentation put = json
				.renameField("originalEmail", JnEntityEmailMessageSent.Fields.email.name())
				.put(JnEntityEmailMessageSent.Fields.subjectType.name(), subjectType);
				
			String language = json.getAsObject(JnEntityEmailTemplateMessage.Fields.language.name());
			
			JnSendMessage sender = new JnSendMessage();
			sender
			.addDefaultProcessForEmailSending()
			.soWithAllAddedProcessAnd()
			.withTheTemplateEntity(this.getClass().getName())
			.andWithTheEntityToBlockMessageResend(JnEntityEmailMessageSent.ENTITY)
			.andWithTheMessageValuesFromJson(put)
			.andWithTheSupportLanguage(language)
			.sendAllMessages()
			;

		
		return json;
	}

}
