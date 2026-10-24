package com.vis.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpExceptionFlow;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.transfer.CcpAfterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterInsertFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpAfterDeleteAnyWhereFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpAfterDeleteFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpAfterSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.JnEntityInstantMessengerParametersToSend;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.entities.JnEntityLoginEmail;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonOnBothSidesEntityBeforeWrite;
import com.jn.json.transformers.JnLoginEmailParentCombinations;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.vis.business.bots.skill.hierarchy.VisSupportSkillFixHierarchyBotConfiguration;
import com.vis.business.skill.VisBusinessSkillFixHierarchyCreateItems;
import com.vis.business.skill.VisBusinessSkillFixHierarchyDeleteOrphanItems;
import com.vis.business.skill.VisBusinessSkillFixHierarchyNotifyAlreadyReviewed;
import com.vis.business.skill.VisBusinessSkillFixHierarchyRefuseAlreadyReviewed;
import com.vis.business.skill.VisErrorSkillFixHierarchyAlreadyReviewed;
import com.vis.business.skill.VisSkillFixHierarchyDecisionNames;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;
import com.vis.messages.VisEmailHtmlFields;
import com.vis.messages.VisMessages;
import com.vis.messages.VisMessages.VisNotifyUserAboutFulfiledSkillHierarchy;

/**
 * Represents pending skill hierarchy fix requests awaiting review: the user asks to associate
 * ({@code type = add}) or dissociate ({@code type = remove}) a skill of their resume with an implicit
 * knowledge ({@code parent}), giving the reason in {@code description}. Inserting a request notifies the
 * user by email and the support bot operator with the {@code /fixSkillHierarchy <type> <email> <parent>} command.
 * A request whose skills were all already reviewed, for the same parent and type, in earlier requests is not
 * saved: {@code VisBusinessSkillFixHierarchyRefuseAlreadyReviewed} refuses it before the save and the global
 * handler emails the user ({@link VisMessages.VisNotifyUserAboutAlreadyReviewedSkillHierarchy}) instead.
 * Every save (insert or update) splits it into one VisEntitySkillFixHierarchyItemPending per skill, and deleting
 * it discards the items no other pending request asks for. The operator's review
 * ({@code VisBusinessSkillFixHierarchyReview}) transfers the request to VisEntitySkillFixHierarchyFulfiled,
 * whatever the decisions were, which emails the user the approved and the rejected items with the operator's
 * justifications.
 * Each request is a ticket of the user in the group of the e-mail ({@code jn_login_email}) of {@code jn_grouper}, as a
 * JSON text, while it waits for the support; the review moves it to the same group of {@code jn_inactive_grouper},
 * with the decision, and giving it up takes it out ({@link JnGrouperChildAsJsonOnBothSidesEntityBeforeWrite}): the
 * screen of the tickets of the user reads both by id. Since 2026-10-23.
 * Versionable, with asynchronous writing and a 1-hour cache.
 */
@CcpEntityGrouperChild(value = {JnEntityLoginEmail.class}, beforeDecoratorClass = JnGrouperChildAsJsonOnBothSidesEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildAsJsonEntityAfterWrite.class)
@CcpEntityGrouperParentCombinations(parent = JnEntityLoginEmail.class, value = JnLoginEmailParentCombinations.class)
@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntitySkillFixHierarchyPending.Fields.class)
@CcpAfterInsertFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError(VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.class)
@CcpAfterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError(messageTemplate = VisNotifyUserAboutFulfiledSkillHierarchy.class, targetEntity = VisEntitySkillFixHierarchyFulfiled.class)
@CcpAfterSaveFromMainEntity(execute = {VisBusinessSkillFixHierarchyCreateItems.class}, operationHandlers = {@CcpExceptionFlow(whenThrowing = VisErrorSkillFixHierarchyAlreadyReviewed.class, thenExecute = {VisBusinessSkillFixHierarchyNotifyAlreadyReviewed.class})})
@CcpAfterDeleteFromMainEntity(execute = {VisBusinessSkillFixHierarchyDeleteOrphanItems.class}, operationHandlers = {@CcpExceptionFlow(whenThrowing = VisErrorSkillFixHierarchyAlreadyReviewed.class, thenExecute = {VisBusinessSkillFixHierarchyNotifyAlreadyReviewed.class})})
@CcpBeforeSaveFromMainEntity(execute = {VisBusinessSkillFixHierarchyRefuseAlreadyReviewed.class}, operationHandlers = {@CcpExceptionFlow(whenThrowing = VisErrorSkillFixHierarchyAlreadyReviewed.class, thenExecute = {VisBusinessSkillFixHierarchyNotifyAlreadyReviewed.class})})
@CcpAfterDeleteAnyWhereFromMainEntity(execute = {VisBusinessSkillFixHierarchyDeleteOrphanItems.class}, operationHandlers = {@CcpExceptionFlow(whenThrowing = VisErrorSkillFixHierarchyAlreadyReviewed.class, thenExecute = {VisBusinessSkillFixHierarchyNotifyAlreadyReviewed.class})})
public class VisEntitySkillFixHierarchyPending implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntitySkillFixHierarchyPending INSTANCE = new VisEntitySkillFixHierarchyPending();

	private VisEntitySkillFixHierarchyPending() {}

	/** The entity {@code vis_skill_fix_hierarchy_pending}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyPending.class).entityInstance;

	/**
	 * Seeds the messages of the {@link VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest}
	 * template, sent when a new request becomes pending: the instant message to the support bot operator with
	 * the {@code fixSkillHierarchy} command (one template per language, with the same text, because it is a
	 * bot command) and its sending parameters (bot and chat), plus the email to the user telling that the
	 * request is being reviewed (Portuguese and English) and its sending parameters. Also seeds the email of
	 * the review result ({@link VisMessages.VisNotifyUserAboutFulfiledSkillHierarchy}) and of the request refused because all of its skills were
	 * already reviewed ({@link VisMessages.VisNotifyUserAboutAlreadyReviewedSkillHierarchy}), and the name of each decision
	 * in the explanation of the reviewed request ({@link VisSkillFixHierarchyDecisionNames}).
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String templateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.class.getName();

		// the parent goes last because it may have spaces (FRONT END): the last parameter of a bot command takes the rest of the text
		String commandToTheOperator = "/fixSkillHierarchy {" + Fields.type + "} {" + Fields.email + "} {" + Fields.parent + "}";
		CcpJsonRepresentation templateWithTemplateId = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation templateWithMessage = templateWithTemplateId
				.put(JnJsonCommonsFields.message, commandToTheOperator);
		CcpJsonRepresentation portugueseTemplate = templateWithMessage
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		CcpJsonRepresentation englishTemplate = templateWithMessage
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

		String typeDescriptionPlaceholder = "{" + VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.JsonFieldNames.typeDescription + "}";
		String skillNamesPlaceholder = "{" + VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.JsonFieldNames.skillNames + "}";
		String parentPlaceholder = "{" + Fields.parent + "}";
		String escapedDescriptionPlaceholder = "{" + VisEmailHtmlFields.escapedDescription + "}";

		// the parent (the implicit knowledge) and the skills are told apart, and the user's justification goes along: up
		// to 2026-10-11 the email read "between the terms ANGULAR, ANT, AOP and AGIL", without telling which was the parent
		String portugueseRequestDetails = "<ul><li><b>Conhecimento implícito (pai):</b> " + parentPlaceholder + "</li>"
				+ "<li><b>Habilidades:</b> " + skillNamesPlaceholder + "</li>"
				+ "<li><b>Sua justificativa:</b> " + escapedDescriptionPlaceholder + "</li></ul>";
		String englishRequestDetails = "<ul><li><b>Implicit knowledge (parent):</b> " + parentPlaceholder + "</li>"
				+ "<li><b>Skills:</b> " + skillNamesPlaceholder + "</li>"
				+ "<li><b>Your justification:</b> " + escapedDescriptionPlaceholder + "</li></ul>";

		String portugueseRequestSentence = "você solicitou " + typeDescriptionPlaceholder + " entre as habilidades " + skillNamesPlaceholder
				+ " e o conhecimento implícito (pai) " + parentPlaceholder;
		String englishRequestSentence = "you requested the " + typeDescriptionPlaceholder + " between the skills " + skillNamesPlaceholder
				+ " and the implicit knowledge (parent) " + parentPlaceholder;

		String portugueseEmailMessage = "<html><body><p>Olá, " + portugueseRequestSentence
				+ ".</p>" + portugueseRequestDetails + "<p>Nosso time está avaliando e te responderá o quanto antes.</p></body></html>";
		CcpJsonRepresentation emailTemplateWithTemplateId = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation portugueseEmailTemplateWithLanguage = emailTemplateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		CcpJsonRepresentation portugueseEmailTemplateWithSubject = portugueseEmailTemplateWithLanguage
				.put(JnJsonCommonsFields.subject, "Recebemos a sua solicitação de ajuste na hierarquia de conhecimentos");
		CcpJsonRepresentation portugueseEmailTemplate = portugueseEmailTemplateWithSubject
				.put(JnJsonCommonsFields.message, portugueseEmailMessage);

		String englishEmailMessage = "<html><body><p>Hello, " + englishRequestSentence
				+ ".</p>" + englishRequestDetails + "<p>Our team is reviewing it and will get back to you as soon as possible.</p></body></html>";
		CcpJsonRepresentation englishEmailTemplateWithLanguage = emailTemplateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.english);
		CcpJsonRepresentation englishEmailTemplateWithSubject = englishEmailTemplateWithLanguage
				.put(JnJsonCommonsFields.subject, "We received your skill hierarchy fix request");
		CcpJsonRepresentation englishEmailTemplate = englishEmailTemplateWithSubject
				.put(JnJsonCommonsFields.message, englishEmailMessage);

		CcpJsonRepresentation emailParametersWithSender = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.sender, "devs.jobsnow@gmail.com");
		CcpJsonRepresentation emailParametersWithSubjectType = emailParametersWithSender
				.put(JnJsonCommonsFields.subjectType, templateId);
		CcpJsonRepresentation emailParameters = emailParametersWithSubjectType
				.put(JnJsonCommonsFields.templateId, templateId);

		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerTemplateMessage.ENTITY, portugueseTemplate, englishTemplate);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerParametersToSend.ENTITY, parametersToTheOperator);
		List<CcpBulkItem> emailTemplateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailTemplateMessage.ENTITY, portugueseEmailTemplate, englishEmailTemplate);
		List<CcpBulkItem> emailParametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailParametersToSend.ENTITY, emailParameters);

		String reviewSummaryPlaceholder = "{" + VisSkillFixHierarchyReviewFields.reviewSummary + "}";
		String portugueseReviewMessage = "<html><body><p>Olá, a sua solicitação de " + typeDescriptionPlaceholder
				+ " com o conhecimento implícito (pai) " + parentPlaceholder + " foi avaliada pelo nosso time.</p>" + portugueseRequestDetails
				+ reviewSummaryPlaceholder + "</body></html>";
		String englishReviewMessage = "<html><body><p>Hello, your " + typeDescriptionPlaceholder
				+ " request with the implicit knowledge (parent) " + parentPlaceholder + " was reviewed by our team.</p>" + englishRequestDetails
				+ reviewSummaryPlaceholder + "</body></html>";
		String portugueseReviewSubject = "A sua solicitação de ajuste na hierarquia de conhecimentos foi avaliada";
		String englishReviewSubject = "Your skill hierarchy fix request was reviewed";

		String fulfiledTemplateId = VisMessages.VisNotifyUserAboutFulfiledSkillHierarchy.class.getName();

		List<CcpBulkItem> reviewEmailItems = this.getEmailTemplateAndParameters(fulfiledTemplateId, portugueseReviewSubject, portugueseReviewMessage, englishReviewSubject, englishReviewMessage);

		String alreadyReviewedTemplateId = VisMessages.VisNotifyUserAboutAlreadyReviewedSkillHierarchy.class.getName();
		String portugueseAlreadyReviewedMessage = "<html><body><p>Olá, " + portugueseRequestSentence
				+ ", mas todas elas já foram atendidas em solicitações anteriores, por isso esta solicitação não foi registrada.</p>"
				+ portugueseRequestDetails + "</body></html>";
		String englishAlreadyReviewedMessage = "<html><body><p>Hello, " + englishRequestSentence
				+ ", but all of them were already handled in earlier requests, so this request was not registered.</p>"
				+ englishRequestDetails + "</body></html>";
		String portugueseAlreadyReviewedSubject = "A sua solicitação de ajuste na hierarquia de conhecimentos já foi atendida";
		String englishAlreadyReviewedSubject = "Your skill hierarchy fix request was already handled";
		List<CcpBulkItem> alreadyReviewedEmailItems = this.getEmailTemplateAndParameters(alreadyReviewedTemplateId, portugueseAlreadyReviewedSubject, portugueseAlreadyReviewedMessage, englishAlreadyReviewedSubject, englishAlreadyReviewedMessage);

		List<CcpBulkItem> firstRecords = new ArrayList<>(templateItems);
		firstRecords.addAll(parametersItems);
		firstRecords.addAll(emailTemplateItems);
		firstRecords.addAll(emailParametersItems);
		firstRecords.addAll(reviewEmailItems);
		firstRecords.addAll(alreadyReviewedEmailItems);

		// the name of each decision beside an item in the explanation of the reviewed request, read in the screen
		List<CcpBulkItem> portugueseApprovedName = VisSkillFixHierarchyDecisionNames.approved.toBulkItems(JnLanguage.portuguese, "aprovado");
		List<CcpBulkItem> englishApprovedName = VisSkillFixHierarchyDecisionNames.approved.toBulkItems(JnLanguage.english, "approved");
		List<CcpBulkItem> portugueseRejectedName = VisSkillFixHierarchyDecisionNames.rejected.toBulkItems(JnLanguage.portuguese, "reprovado");
		List<CcpBulkItem> englishRejectedName = VisSkillFixHierarchyDecisionNames.rejected.toBulkItems(JnLanguage.english, "rejected");
		firstRecords.addAll(portugueseApprovedName);
		firstRecords.addAll(englishApprovedName);
		firstRecords.addAll(portugueseRejectedName);
		firstRecords.addAll(englishRejectedName);
		// the command of the support bot that reviews these requests: since 2026-10-15 vis seeds it, and not jb
		List<CcpBulkItem> reviewCommandItems = VisSupportSkillFixHierarchyBotConfiguration.getSeeds();
		firstRecords.addAll(reviewCommandItems);
		return firstRecords;
	}

	/**
	 * Email template (Portuguese and English) and sending parameters of the given template id.
	 */
	private List<CcpBulkItem> getEmailTemplateAndParameters(String templateId, String portugueseSubject, String portugueseMessage, String englishSubject, String englishMessage) {

		CcpJsonRepresentation templateWithTemplateId = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId);

		CcpJsonRepresentation portugueseTemplateWithLanguage = templateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		CcpJsonRepresentation portugueseTemplateWithSubject = portugueseTemplateWithLanguage
				.put(JnJsonCommonsFields.subject, portugueseSubject);
		CcpJsonRepresentation portugueseTemplate = portugueseTemplateWithSubject
				.put(JnJsonCommonsFields.message, portugueseMessage);

		CcpJsonRepresentation englishTemplateWithLanguage = templateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.english);
		CcpJsonRepresentation englishTemplateWithSubject = englishTemplateWithLanguage
				.put(JnJsonCommonsFields.subject, englishSubject);
		CcpJsonRepresentation englishTemplate = englishTemplateWithSubject
				.put(JnJsonCommonsFields.message, englishMessage);

		CcpJsonRepresentation parametersWithSender = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.sender, "devs.jobsnow@gmail.com");
		CcpJsonRepresentation parametersWithSubjectType = parametersWithSender
				.put(JnJsonCommonsFields.subjectType, templateId);
		CcpJsonRepresentation parameters = parametersWithSubjectType
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
		/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The {@code description} field: validated as in {@code JnJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		description,

		/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, part of the primary key. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		parent,

		/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, list, required. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		skill,

		/** The {@code type} field: text, part of the primary key. */
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSkillFixHierarchyTypes.class)
		@CcpEntityFieldPrimaryKey
		type,

		/**
		 * The {@code date} field: text, the date and time of the last save ({@code dd/MM/yyyy HH:mm:ss}), filled by the
		 * decorator of {@code @CcpEntityGrouperChild}; it goes with the request to the tickets of the user. Since 2026-10-23.
		 */
		@CcpJsonFieldTypeString(exactLength = 19)
		date,

	}
}
