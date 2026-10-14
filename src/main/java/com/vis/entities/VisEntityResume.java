package com.vis.entities;

import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterDeleteAnyWhereFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterDeleteFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterDeleteFromTwinEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterSaveFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning;

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
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.decorators.annotations.JnEntityAsyncWriter;
import com.jn.entities.decorators.annotations.JnEntityGrouperChild;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenWrite;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenWriteOperation;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserAfterWriteBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserBeforeWriteBuilder;
import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.builders.JnEntityAsyncWriterBuilder;
import com.jn.entities.decorators.builders.JnEntityGrouperChildAfterWriteBuilder;
import com.jn.entities.decorators.builders.JnEntityGrouperChildBeforeWriteBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.json.transformers.VisResumeHashCombinations;
import com.vis.messages.VisMessages;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.transformers.VisJsonTransformerExperienceStartYear;
import com.vis.json.transformers.VisJsonTransformerResumeHashes;
import com.vis.json.transformers.VisJsonTransformerSpokenLanguageWithoutId;

/**
 * Represents the candidate's core Resume entity. Stores the complete professional profile:
 * desired contract type (CLT/PJ/BTC), availability, DDDs of interest, skills, the year the experience started (the
 * seniority is calculated from it), LinkedIn, spoken languages, company restrictions and time available for temporary
 * work; date and time of each version live in the versionable copy. Uses Twin Entity
 * to control inactive resumes, asynchronous writing, versioning and a 1-hour cache.
 *
 * <p>Life cycle (the delete moves the resume to the twin, {@code vis_inactive_resume}; the delete on the twin moves it
 * back, which is the reactivation):
 * <ul>
 * <li>the field transformers fill the {@code hash} list with every search the resume satisfies;</li>
 * <li>{@code @JnEntityGrouperChild} keeps the id of the resume in the groups of {@code JnEntityGrouper} of its hashes:
 * the active resumes in the main groups, the inactive ones in the twin groups ({@code jn_inactive_grouper}); the save
 * moves it to the groups of its new hashes, the deactivation and the reactivation move it between the sides, and the
 * delete everywhere takes it out of both;</li>
 * <li>the owner is told by e-mail after the save, the deactivation, the reactivation and the delete everywhere.</li>
 * </ul>
 * A notice that fails to go out saves a warning instead of undoing the write.
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {
		@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),
		@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),
		@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8),
		@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserBeforeWriteBuilder.class, priority = 7),
		@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserAfterWriteBuilder.class, priority = 5),
		@CcpEntityCustomDecorator(value = JnEntityGrouperChildBeforeWriteBuilder.class, priority = 7),
		@CcpEntityCustomDecorator(value = JnEntityGrouperChildAfterWriteBuilder.class, priority = 5),
		})
@JnEntitySendMessageToUserWhenWrite({
	@JnEntitySendMessageToUserWhenWriteOperation(
			operationType = afterSaveFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning,
			messageTemplate = VisMessages.VisNotifyUserAboutSavedResume.class
			),
	@JnEntitySendMessageToUserWhenWriteOperation(
			operationType = afterDeleteFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning,
			messageTemplate = VisMessages.VisNotifyUserAboutDeactivatedResume.class
			),
	@JnEntitySendMessageToUserWhenWriteOperation(
			operationType = afterDeleteFromTwinEntitySendAnEmailMessageAndIfFailsSaveAWarning,
			messageTemplate = VisMessages.VisNotifyUserAboutReactivatedResume.class
			),
	@JnEntitySendMessageToUserWhenWriteOperation(
			operationType = afterDeleteAnyWhereFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning,
			messageTemplate = VisMessages.VisNotifyUserAboutDeletedResume.class
			),
})
@CcpEntityTwin(
		twinEntityName = "vis_inactive_resume",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		) 
@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityResume.Fields.class)
// one group per hash (each search the resume satisfies): the combinations of its values, the entity that gives the id
// of each one (and names the groups) and the list where the transformers keep those ids
@JnEntityGrouperChild(
		parentsCombinations = VisResumeHashCombinations.class,
		parentIdEntity = VisEntityVirtualHashGrouper.class,
		storedParentIdsField = "hash"
		)
public class VisEntityResume implements CcpEntityConfigurator {
	
	/** The entity {@code vis_resume}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;

	/**
	 * Seeds the e-mails to the owner of the resume (Portuguese and English) and their sending parameters: saved,
	 * deactivated, reactivated and deleted everywhere.
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> items = new ArrayList<>();

		String savedTemplateId = VisMessages.VisNotifyUserAboutSavedResume.class.getName();
		List<CcpBulkItem> savedItems = this.getEmailTemplateAndParameters(savedTemplateId,
				"O seu currículo foi salvo",
				"<html><body><p>Olá, o seu currículo foi salvo no JobsNow e já está disponível para os recrutadores.</p></body></html>",
				"Your resume was saved",
				"<html><body><p>Hello, your resume was saved on JobsNow and is now available to the recruiters.</p></body></html>");
		items.addAll(savedItems);

		String deactivatedTemplateId = VisMessages.VisNotifyUserAboutDeactivatedResume.class.getName();
		List<CcpBulkItem> deactivatedItems = this.getEmailTemplateAndParameters(deactivatedTemplateId,
				"O seu currículo foi desativado",
				"<html><body><p>Olá, o seu currículo foi desativado no JobsNow e deixou de aparecer para os recrutadores. Você pode reativá-lo quando quiser.</p></body></html>",
				"Your resume was deactivated",
				"<html><body><p>Hello, your resume was deactivated on JobsNow and no longer shows up to the recruiters. You can reactivate it whenever you want.</p></body></html>");
		items.addAll(deactivatedItems);

		String reactivatedTemplateId = VisMessages.VisNotifyUserAboutReactivatedResume.class.getName();
		List<CcpBulkItem> reactivatedItems = this.getEmailTemplateAndParameters(reactivatedTemplateId,
				"O seu currículo foi reativado",
				"<html><body><p>Olá, o seu currículo foi reativado no JobsNow e voltou a aparecer para os recrutadores.</p></body></html>",
				"Your resume was reactivated",
				"<html><body><p>Hello, your resume was reactivated on JobsNow and shows up to the recruiters again.</p></body></html>");
		items.addAll(reactivatedItems);

		String deletedTemplateId = VisMessages.VisNotifyUserAboutDeletedResume.class.getName();
		List<CcpBulkItem> deletedItems = this.getEmailTemplateAndParameters(deletedTemplateId,
				"Os dados do seu currículo foram excluídos",
				"<html><body><p>Olá, todos os dados do seu currículo foram excluídos do JobsNow de forma permanente.</p></body></html>",
				"Your resume data was deleted",
				"<html><body><p>Hello, every data of your resume was permanently deleted from JobsNow.</p></body></html>");
		items.addAll(deletedItems);

		return items;
	}

	/**
	 * Builds the e-mail template (Portuguese and English) and the sending parameters of a template id.
	 * @param templateId the template id
	 * @param portugueseSubject the subject in Portuguese
	 * @param portugueseMessage the message in Portuguese
	 * @param englishSubject the subject in English
	 * @param englishMessage the message in English
	 * @return the bulk items of the templates and of the parameters
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

	/** The salary fields of the resume. */
	public static enum SalaryType{
		/** The value as a contractor (PJ). */
		pj,
		/** The salary as an employee (CLT). */
		clt
	}
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	@CcpJsonGlobalValidations(
			requiresAtLeastOne = {
			@CcpJsonValidationFieldList(SalaryType.class) 
	})
	public static enum Fields implements CcpJsonFieldName {

		/** The {@code btc} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		btc,

		/** The {@code clt} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		clt,

		/** The {@code disponibility} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility,

		/** The {@code ddd} field: required, list, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/** The {@code desiredJob} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		desiredJob,

		/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/**
		 * The {@code experience} field: required, the years of experience (0 to 70) on input; transformed by
		 * {@code VisJsonTransformerExperienceStartYear}, it is stored as the year the experience started.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned(maxValue = 70)
		@CcpEntityFieldTransformer(VisJsonTransformerExperienceStartYear.class)
		experience,

		/**
		 * The {@code hash} field: list, validated as in {@code VisJsonCommonsFields}; calculated by
		 * {@code VisJsonTransformerResumeHashes}, it holds the id of every search the resume satisfies.
		 */
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerResumeHashes.class)
		hash,

		/** The {@code skill} field: list, nested JSON. */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSkills.class)
		skill,

		/** The {@code lastJob} field: text. */
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 50)
		lastJob,

		/**
		 * The {@code spokenLanguage} field: list, validated as in {@code VisJsonCommonsFields}; transformed by
		 * {@code VisJsonTransformerSpokenLanguageWithoutId}, which drops the {@code id} of each language.
		 */
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerSpokenLanguageWithoutId.class)
		spokenLanguage,

		/** The {@code linkedinAddress} field: required, text. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(regexValidation = "^https://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-_%]+/?$")
		linkedinAddress,

		/** The {@code negotiableClaim} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		negotiableClaim,

		/** The {@code notAllowedCompany} field: list, text. */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 2, maxLength = 20)
		notAllowedCompany,

		/** The {@code pcd} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		pcd,

		/** The {@code pj} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		pj,

		/** The {@code resumeType} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,

		/** The {@code temporallyJobTime} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		temporallyJobTime,

		/** The {@code travel} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		travel,
		;
	}	
}

