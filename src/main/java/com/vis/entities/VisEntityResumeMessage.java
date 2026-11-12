package com.vis.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterSaveFromMainEntitySendAnInstantMessageAndIfFailsSaveAWarning;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.business.messages.JnEmailFooter;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.JnEntityInstantMessengerParametersToSend;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.vis.business.bots.VisSupportBotCommands;
import com.vis.business.bots.linkedin.VisSupportVerifyLinkedinBotConfiguration;
import com.vis.business.bots.linkedin.VisSupportVerifyLinkedinFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisResumeMessageTypes;
import com.vis.messages.VisMessages;

/**
 * The messages to the owner of a resume through the user bot (since 2026-11-12): one per chat ({@code chatId}) and kind
 * ({@code messageType}, {@link VisResumeMessageTypes}), with the text already in the language of the owner
 * ({@code message}). Each save sends the text to the chat ({@link VisMessages.VisInvalidLinkedinAddress}); the record
 * prevails over the text of the template. Versioned, so every message sent stays in the history.
 * <p>
 * Seeds the whole flow of the LinkedIn address reported as invalid by a recruiter: the template and the parameters of
 * the message to the owner (user bot), of the ticket to the support operator ({@code /verifyLinkedinAddress}), of the
 * e-mail to the recruiter when the address exists, and the command of the support bot
 * ({@link VisSupportVerifyLinkedinBotConfiguration}).
 */
@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsValidator(VisEntityResumeMessage.Fields.class)
@CcpAfterSaveFromMainEntitySendAnInstantMessageAndIfFailsSaveAWarning(VisMessages.VisInvalidLinkedinAddress.class)
public class VisEntityResumeMessage implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityResumeMessage INSTANCE = new VisEntityResumeMessage();

	private VisEntityResumeMessage() {}

	/** The entity {@code vis_resume_message}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeMessage.class).entityInstance;

	/** The chat of the support operator, who receives the tickets. */
	static final long OPERATOR_CHAT_ID = 751717896L;

	/** The chat of the parameters of the user bot: a placeholder, the chat of each message prevails. */
	static final long NO_CHAT_ID = 0L;

	/** The sender of the e-mails about resumes. */
	static final String SENDER = "curriculos@onias.site";

	/**
	 * Seeds the templates and the parameters of the flow of the LinkedIn address reported as invalid, and the command
	 * {@code /verifyLinkedinAddress}.
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String linkedinPlaceholder = "{" + VisJsonCommonsFields.linkedinAddress + "}";
		List<CcpBulkItem> items = new ArrayList<>();

		String ownerTemplateId = VisMessages.VisInvalidLinkedinAddress.class.getName();
		String ownerPortuguese = "O endereço de linkedin " + linkedinPlaceholder + " que você informou foi reportado como inválido por recrutador "
				+ "e depois foi confirmada a sua invalidez pelo nosso time de suporte. Enquanto este erro continuar, não poderemos "
				+ "permitir ou contabilizar visualização ao seu perfil e nem poderemos enviar o seu perfil aos recrutadores. "
				+ "Por isso, pedimos que corrija a falha.";
		String ownerEnglish = "The LinkedIn address " + linkedinPlaceholder + " you informed was reported as invalid by a recruiter "
				+ "and then confirmed as invalid by our support team. While this error lasts, we cannot allow or count views of your "
				+ "profile, nor send your profile to the recruiters. So we ask you to fix it.";
		List<CcpBulkItem> ownerItems = this.getInstantMessageTemplateAndParameters(ownerTemplateId, ownerPortuguese, ownerEnglish, JnMessageType.JnBotType.user);
		items.addAll(ownerItems);

		// the command is the same in every language
		String ticketTemplateId = VisMessages.VisNotifySupportAboutInvalidLinkedinTicket.class.getName();
		String ticketCommand = "/" + VisSupportBotCommands.verifyLinkedinAddress + " " + linkedinPlaceholder
				+ " {" + VisSupportVerifyLinkedinFields.resumeEmailSha1 + "} {" + VisSupportVerifyLinkedinFields.recruiterEmail + "}";
		List<CcpBulkItem> ticketItems = this.getInstantMessageTemplateAndParameters(ticketTemplateId, ticketCommand, ticketCommand, JnMessageType.JnBotType.support);
		items.addAll(ticketItems);

		String recruiterTemplateId = VisMessages.VisNotifyRecruiterAboutUnreachableLinkedin.class.getName();
		String recruiterPortuguese = "<html><body>"
				+ "<p>Olá. Você nos reportou que o LinkedIn " + linkedinPlaceholder + " de um currículo do Onias.Site não abria.</p>"
				+ "<p>O nosso time de suporte verificou: o perfil existe no LinkedIn. Pode ser que apenas você não tenha permissão de ver "
				+ "aquele perfil. Por isso, este currículo não será mais enviado a você.</p>"
				+ JnEmailFooter.portuguese.getValue()
				+ "</body></html>";
		String recruiterEnglish = "<html><body>"
				+ "<p>Hello. You reported to us that the LinkedIn " + linkedinPlaceholder + " of a resume of Onias.Site did not open.</p>"
				+ "<p>Our support team checked it: the profile exists on LinkedIn. Maybe only you are not allowed to see that profile. "
				+ "So this resume will no longer be sent to you.</p>"
				+ JnEmailFooter.english.getValue()
				+ "</body></html>";
		List<CcpBulkItem> recruiterItems = this.getEmailTemplateAndParameters(recruiterTemplateId, "Sobre o LinkedIn que você reportou", recruiterPortuguese,
				"About the LinkedIn you reported", recruiterEnglish);
		items.addAll(recruiterItems);

		List<CcpBulkItem> commandItems = VisSupportVerifyLinkedinBotConfiguration.getSeeds();
		items.addAll(commandItems);
		return items;
	}

	/**
	 * The instant message template (Portuguese and English) and the sending parameters of a template id. The support bot
	 * sends to the chat of the operator; the user bot, to the {@code chatId} of the record.
	 * @param templateId the class name of the message
	 * @param portugueseMessage the text in Portuguese
	 * @param englishMessage the text in English
	 * @param bot the bot that sends
	 * @return the bulk items
	 */
	private List<CcpBulkItem> getInstantMessageTemplateAndParameters(String templateId, String portugueseMessage, String englishMessage, JnMessageType.JnBotType bot) {
		CcpJsonRepresentation templateWithTemplateId = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation portugueseTemplate = templateWithTemplateId
				.put(JnJsonCommonsFields.message, portugueseMessage)
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		CcpJsonRepresentation englishTemplate = templateWithTemplateId
				.put(JnJsonCommonsFields.message, englishMessage)
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
				.put(JnJsonInstantMessengerFields.botName, bot);
		// the parameters require a chat: the user bot gets 0, a placeholder, since the chatId of the record prevails
		boolean toTheOperator = JnMessageType.JnBotType.support == bot;
		Long chatId = toTheOperator ? OPERATOR_CHAT_ID : NO_CHAT_ID;
		CcpJsonRepresentation parameters = parametersWithBotName.put(JnJsonInstantMessengerFields.chatId, chatId);

		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerTemplateMessage.ENTITY, portugueseTemplate, englishTemplate);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerParametersToSend.ENTITY, parameters);
		List<CcpBulkItem> templateAndParameters = new ArrayList<>(templateItems);
		templateAndParameters.addAll(parametersItems);
		return templateAndParameters;
	}

	/**
	 * The e-mail template (Portuguese and English) and the sending parameters of a template id.
	 * @param templateId the class name of the message
	 * @param portugueseSubject the subject in Portuguese
	 * @param portugueseMessage the message in Portuguese
	 * @param englishSubject the subject in English
	 * @param englishMessage the message in English
	 * @return the bulk items
	 */
	private List<CcpBulkItem> getEmailTemplateAndParameters(String templateId, String portugueseSubject, String portugueseMessage, String englishSubject, String englishMessage) {
		CcpJsonRepresentation templateWithTemplateId = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation portugueseTemplate = templateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese)
				.put(JnJsonCommonsFields.subject, portugueseSubject)
				.put(JnJsonCommonsFields.message, portugueseMessage);
		CcpJsonRepresentation englishTemplate = templateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.english)
				.put(JnJsonCommonsFields.subject, englishSubject)
				.put(JnJsonCommonsFields.message, englishMessage);
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.sender, SENDER)
				.put(JnJsonCommonsFields.subjectType, templateId)
				.put(JnJsonCommonsFields.templateId, templateId);
		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailTemplateMessage.ENTITY, portugueseTemplate, englishTemplate);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailParametersToSend.ENTITY, parameters);
		List<CcpBulkItem> templateAndParameters = new ArrayList<>(templateItems);
		templateAndParameters.addAll(parametersItems);
		return templateAndParameters;
	}

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code chatId} field: part of the primary key, a number as the chat of the bots, the chat of the owner of the resume in the user bot. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		chatId,

		/** The {@code messageType} field: part of the primary key, one of {@link VisResumeMessageTypes}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = VisResumeMessageTypes.class)
		messageType,

		/** The {@code message} field: required, text of 10 to 500 characters, the message sent to the owner. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 500)
		message,
		;
	}
}
