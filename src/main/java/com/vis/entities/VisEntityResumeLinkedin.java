package com.vis.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityInstantMessengerParametersToSend;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.messages.VisMessages;

/**
 * The resumes that use each LinkedIn address ({@code linkedinAddress}, the key; {@code emails}, the hashes of their
 * e-mails), so that saving a resume finds by id whether the address is already in the resume of another e-mail (the
 * online save cannot search the address in {@link VisEntityResume}). Written by {@code VisBusinessResumeLinkedinOwner}
 * after each save of a resume, which adds the e-mail to the address; when the address then has more than one e-mail, the
 * support bot operator is told ({@code VisNotifySupportAboutSharedLinkedin}), because two resumes with the same LinkedIn
 * may be someone using a profile that is not theirs. The resume is saved anyway (decision of 2026-10-15: refusing would
 * block whoever changed e-mail). Changing the address or deleting the resume everywhere takes only its own e-mail out
 * ({@code VisBusinessResumeLinkedinRelease}), and the record goes away with the last e-mail.
 * <p>
 * Until 2026-10-15 it kept a single {@code email}, the last resume that saved the address: a resume that borrowed an
 * address and gave it back deleted the record, and the original owner, still using it, was left without one (finding
 * 24). Both writes are atomic scripts run by the database, as in the groupers, so resumes saving the same address at the
 * same time do not overwrite each other.
 */
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityResumeLinkedin.Fields.class)
public class VisEntityResumeLinkedin implements CcpEntityConfigurator {

	/** The entity {@code vis_resume_linkedin}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeLinkedin.class).entityInstance;

	/**
	 * Seeds the notice to the support bot operator about a LinkedIn address saved by two resumes (one template per
	 * language) and its sending parameters.
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String templateId = VisMessages.VisNotifySupportAboutSharedLinkedin.class.getName();

		String linkedinPlaceholder = "{" + Fields.linkedinAddress + "}";
		String emailPlaceholder = "{" + JnJsonCommonsFields.email + "}";
		String portugueseMessage = "O LinkedIn " + linkedinPlaceholder + " já estava no currículo de outro e-mail e acaba de ser salvo no currículo de "
				+ emailPlaceholder + ". O currículo foi salvo; verifique se o perfil pertence a outra pessoa.";
		String englishMessage = "The LinkedIn " + linkedinPlaceholder + " was already in the resume of another e-mail and has just been saved in the resume of "
				+ emailPlaceholder + ". The resume was saved; check whether the profile belongs to someone else.";

		CcpJsonRepresentation templateWithTemplateId = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation portugueseTemplateWithMessage = templateWithTemplateId
				.put(JnJsonCommonsFields.message, portugueseMessage);
		CcpJsonRepresentation portugueseTemplate = portugueseTemplateWithMessage
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		CcpJsonRepresentation englishTemplateWithMessage = templateWithTemplateId
				.put(JnJsonCommonsFields.message, englishMessage);
		CcpJsonRepresentation englishTemplate = englishTemplateWithMessage
				.put(JnJsonCommonsFields.language, JnLanguage.english);

		CcpJsonRepresentation parametersWithInstantMessageType = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonInstantMessengerFields.instantMessageType, JnInstantMessageType.text);
		CcpJsonRepresentation parametersWithTemplateId = parametersWithInstantMessageType
				.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation parametersWithMaxTries = parametersWithTemplateId
				.addToItem(JnJsonCommonsFields.moreParameters, JnJsonCommonsFields.maxTriesToSendMessage, 10);
		CcpJsonRepresentation parametersWithSleepTime = parametersWithMaxTries
				.addToItem(JnJsonCommonsFields.moreParameters, JnJsonCommonsFields.sleepToSendMessage, 3000);
		CcpJsonRepresentation parametersWithBotName = parametersWithSleepTime
				.put(JnJsonInstantMessengerFields.botName, JnMessageType.JnBotType.support);
		CcpJsonRepresentation parametersToTheOperator = parametersWithBotName
				.put(JnJsonInstantMessengerFields.chatId, 751717896L);

		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerTemplateMessage.ENTITY, portugueseTemplate, englishTemplate);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerParametersToSend.ENTITY, parametersToTheOperator);
		List<CcpBulkItem> firstRecords = new ArrayList<>(templateItems);
		firstRecords.addAll(parametersItems);
		return firstRecords;
	}

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code linkedinAddress} field: the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		linkedinAddress,

		/** The {@code emails} field: the hashes of the e-mails of the resumes that use the address, required. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 128)
		emails,
		;
	}
}
