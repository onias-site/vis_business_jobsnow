package com.vis.entities;

import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenTransferOperationType.afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError;
import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterInsertFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDataTransfer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDataTransfers;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDataTransferType;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.business.messages.JnInstantMessageType;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.JnEntityInstantMessengerParametersToSend;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.entities.decorators.annotations.JnEntityAsyncWriter;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenTransfer;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenTransferOperation;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenWrite;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenWriteOperation;
import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.builders.JnEntityAsyncWriterBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserAfterTransferBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserAfterWriteBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserBeforeTransferBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserBeforeWriteBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillSuggestionApply;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.messages.VisMessages;
import com.vis.messages.VisMessages.VisNotifyUserAboutAprovedSkill;
import com.vis.messages.VisMessages.VisNotifyUserAboutRejectedSkill;

/**
 * Skills suggested by candidates that are not in {@link VisEntitySkill} yet, waiting for the review of the support
 * bot operator: the candidate ({@code email}) suggests a {@code skill} that is in the text of their resume and that
 * was not listed, its synonyms ({@code synonym}) and why ({@code description}). One suggestion per candidate and
 * skill. Inserting a suggestion emails the candidate and sends the operator the
 * {@code /reviewSkillSuggestion <email> <skill>} command. The review transfers the suggestion to
 * {@link VisEntitySkillApproved} or to {@link VisEntitySkillRejected}, with the operator's justification
 * ({@code explanation}), which emails the candidate the result; the approval also puts the skill in
 * {@link VisEntitySkill} ({@code VisBusinessSkillSuggestionApply}).
 * Versionable, with asynchronous writing and a 1-hour cache.
 */
@CcpEntityCache(3600)

@CcpEntityCustomDecorators(value = {
		@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5)
		,@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserBeforeWriteBuilder.class, priority = 7)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserBeforeTransferBuilder.class, priority = 7)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserAfterWriteBuilder.class, priority = 5)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserAfterTransferBuilder.class, priority = 5)
})

@JnEntitySendMessageToUserWhenWrite({
	@JnEntitySendMessageToUserWhenWriteOperation(
			operationType = afterInsertFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError,
			messageTemplate =  VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class
			),
})
@JnEntitySendMessageToUserWhenTransfer(
		{
			@JnEntitySendMessageToUserWhenTransferOperation
			(
				operationType = afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError,
				messageTemplate = VisNotifyUserAboutRejectedSkill.class,
				targetEntity = VisEntitySkillRejected.class
			),
			@JnEntitySendMessageToUserWhenTransferOperation
			(
				operationType = afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError,
				messageTemplate = VisNotifyUserAboutAprovedSkill.class,
				targetEntity = VisEntitySkillApproved.class
			),
		}
		)
@CcpEntityDataTransfers({
		@CcpEntityDataTransfer(operationType = CcpEntityDataTransferType.afterTransferDataFromMainEntity, targetEntity = VisEntitySkillApproved.class, execute = {VisBusinessSkillSuggestionApply.class}, transferHandlers = {}),
})
@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillPending.Fields.class)
public class VisEntitySkillPending implements CcpEntityConfigurator {

	/** The entity {@code vis_skill_pending}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillPending.class).entityInstance;

	/**
	 * Seeds the messages of the skill suggestion: the instant message to the support bot operator with the
	 * {@code reviewSkillSuggestion} command (one template per language, with the same text, because it is a bot
	 * command) and its sending parameters, the email to the candidate telling that the suggestion is being reviewed,
	 * and the emails of the approval and of the rejection (Portuguese and English), with their sending parameters.
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String templateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class.getName();

		// the skill goes last because it may have spaces: the last parameter of a bot command takes the rest of the text
		String commandToTheOperator = "/reviewSkillSuggestion {" + Fields.email + "} {" + Fields.skill + "}";
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

		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerTemplateMessage.ENTITY, portugueseTemplate, englishTemplate);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityInstantMessengerParametersToSend.ENTITY, parametersToTheOperator);

		String skillPlaceholder = "{" + Fields.skill + "}";
		String synonymNamesPlaceholder = "{" + VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.JsonFieldNames.synonymNames + "}";
		String explanationPlaceholder = "{" + JnJsonCommonsFields.explanation + "}";

		String portuguesePendingMessage = "<html><body><p>Olá, você sugeriu a habilidade " + skillPlaceholder
				+ " (sinônimos: " + synonymNamesPlaceholder + "). Nosso time está avaliando e te responderá o quanto antes.</p></body></html>";
		String englishPendingMessage = "<html><body><p>Hello, you suggested the skill " + skillPlaceholder
				+ " (synonyms: " + synonymNamesPlaceholder + "). Our team is reviewing it and will get back to you as soon as possible.</p></body></html>";
		List<CcpBulkItem> pendingEmailItems = this.getEmailTemplateAndParameters(templateId
				, "Recebemos a sua sugestão de habilidade", portuguesePendingMessage
				, "We received your skill suggestion", englishPendingMessage);

		String approvedTemplateId = VisMessages.VisNotifyUserAboutAprovedSkill.class.getName();
		String portugueseApprovedMessage = "<html><body><p>Olá, a habilidade " + skillPlaceholder
				+ " que você sugeriu foi aprovada pelo nosso time e passa a ser reconhecida nos currículos.</p><p>" + explanationPlaceholder + "</p></body></html>";
		String englishApprovedMessage = "<html><body><p>Hello, the skill " + skillPlaceholder
				+ " you suggested was approved by our team and is now recognized in the resumes.</p><p>" + explanationPlaceholder + "</p></body></html>";
		List<CcpBulkItem> approvedEmailItems = this.getEmailTemplateAndParameters(approvedTemplateId
				, "A sua sugestão de habilidade foi aprovada", portugueseApprovedMessage
				, "Your skill suggestion was approved", englishApprovedMessage);

		String rejectedTemplateId = VisMessages.VisNotifyUserAboutRejectedSkill.class.getName();
		String portugueseRejectedMessage = "<html><body><p>Olá, a habilidade " + skillPlaceholder
				+ " que você sugeriu não foi aprovada pelo nosso time.</p><p>" + explanationPlaceholder + "</p></body></html>";
		String englishRejectedMessage = "<html><body><p>Hello, the skill " + skillPlaceholder
				+ " you suggested was not approved by our team.</p><p>" + explanationPlaceholder + "</p></body></html>";
		List<CcpBulkItem> rejectedEmailItems = this.getEmailTemplateAndParameters(rejectedTemplateId
				, "A sua sugestão de habilidade não foi aprovada", portugueseRejectedMessage
				, "Your skill suggestion was not approved", englishRejectedMessage);

		List<CcpBulkItem> firstRecords = new ArrayList<>(templateItems);
		firstRecords.addAll(parametersItems);
		firstRecords.addAll(pendingEmailItems);
		firstRecords.addAll(approvedEmailItems);
		firstRecords.addAll(rejectedEmailItems);
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
	 * {@code @CcpEntityFieldsValidator}). {@link VisEntitySkillApproved} and {@link VisEntitySkillRejected} have
	 * the same fields plus the operator's {@code explanation}.
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The {@code skill} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill,

		/** The {@code synonym} field: validated as in {@code VisJsonCommonsFields}, list. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,

		/** The {@code description} field: why the candidate suggests the skill, validated as in {@code JnJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		description,
		;
	}
}
