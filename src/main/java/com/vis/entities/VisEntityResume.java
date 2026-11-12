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
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityReplacedOnSave;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChildCounter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpStoredParentIdsField;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityRecordsCounter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterDeleteAnyWhereFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterDeleteFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterDeleteFromTwinEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterSaveFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpAfterSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeDeleteAnyWhereFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeSaveFromMainEntity;
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
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnGrouperChildCurrentCounterEntityBeforeWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityBeforeWrite;
import com.jn.entities.decorators.engine.JnInsertionDateEntity;
import com.jn.entities.fields.counters.JnInsertionYearMonthCounterKeys;
import com.vis.json.transformers.VisInsertionMonthCounterCombinations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityInsertionDate;
import com.jn.entities.decorators.engine.JnRecordsCounterEntity;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.jn.business.messages.JnEmailFooter;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.business.resume.VisBusinessResumeLinkedinOwner;
import com.vis.business.resume.VisBusinessResumeLinkedinRelease;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisJsonValidatorHomeOfficeAlone;
import com.vis.json.transformers.VisJsonTransformerExperienceStartYear;
import com.vis.json.transformers.VisJsonTransformerSpokenLanguageWithoutId;
import com.vis.json.transformers.VisResumeDddCombinations;
import com.vis.json.transformers.VisResumeSkillsFromResumeChildField;
import com.vis.json.transformers.VisResumeExcludedSkillCombinations;
import com.vis.json.transformers.VisResumeExcludedSkillCounterKeys;
import com.vis.json.transformers.VisResumeHashCombinations;
import com.vis.json.transformers.VisResumeNotAllowedCompanyCounterCombinations;
import com.vis.json.transformers.VisResumeNotAllowedCompanyCounterKeys;
import com.vis.messages.VisMessages;

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
 * <li>the save fills the {@code hash} list with every search the resume satisfies;</li>
 * <li>a second {@code @CcpEntityGrouperChild} keeps, in the group of each ddd of the resume ({@link VisEntityVirtualDddGrouper}),
 * a JSON text with btc, clt, disponibility, experience, spokenLanguage, pcd, pj, resumeType, temporallyJobTime, travel and
 * the id of the resume, moved between the sides as the groups of the hashes are;</li>
 * <li>{@code @CcpEntityGrouperChild} keeps the id of the resume in the groups of {@code JnEntityGrouper} of its hashes:
 * the active resumes in the main groups, the inactive ones in the twin groups ({@code jn_inactive_grouper}); the save
 * moves it to the groups of its new hashes, the deactivation and the reactivation move it between the sides, and the
 * delete everywhere takes it out of both;</li>
 * <li>a second {@code @CcpEntityGrouperChildCounter} counts, in the group of each skill the owner excluded
 * ({@link VisEntityVirtualExcludedSkillCounter}), how many active resumes exclude it now: the most avoided skills;</li>
 * <li>the owner is told by e-mail after the save, the deactivation, the reactivation and the delete everywhere;</li>
 * <li>{@link VisEntityResumeLinkedin} keeps the e-mails that use each LinkedIn address: saving an address that is in
 * the resume of another e-mail saves the resume anyway and tells the support operator; a new address or the delete
 * everywhere takes only this e-mail out of the old address.</li>
 * </ul>
 * A notice that fails to go out saves a warning instead of undoing the write.
 */
@CcpEntityCache(3600)
@CcpEntityReplacedOnSave
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsValidator(VisEntityResume.Fields.class)
@CcpAfterSaveFromMainEntity(execute = {VisBusinessResumeLinkedinOwner.class}, operationHandlers = {})
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpBeforeDeleteAnyWhereFromMainEntity(execute = {VisBusinessResumeLinkedinRelease.class}, operationHandlers = {})
@CcpAfterSaveFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning(VisMessages.VisNotifyUserAboutSavedResume.class)
@CcpAfterDeleteFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning(VisMessages.VisNotifyUserAboutDeactivatedResume.class)
@CcpAfterDeleteFromTwinEntitySendAnEmailMessageAndIfFailsSaveAWarning(VisMessages.VisNotifyUserAboutReactivatedResume.class)
@CcpAfterDeleteAnyWhereFromMainEntitySendAnEmailMessageAndIfFailsSaveAWarning(VisMessages.VisNotifyUserAboutDeletedResume.class)
// how many active (vis_resume) and inactive (vis_inactive_resume) resumes there are now, in jn_records_count, counted
// again after each insertion, deactivation, reactivation and delete
@CcpEntityRecordsCounter(JnRecordsCounterEntity.class)
@CcpEntityTwin(
		twinEntityName = "vis_inactive_resume",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
// the e-mails that use each LinkedIn address, read by id on the save: an address already used by another e-mail
// reaches the support operator (VisEntityResumeLinkedin)
@CcpBeforeSaveFromMainEntity(execute = {VisBusinessResumeLinkedinRelease.class}, operationHandlers = {})
// one group per hash (each search the resume satisfies): the entity that gives the id of each one (and names the
// groups); the combinations of its values and the list where the save keeps those ids come from the field hash
@CcpEntityGrouperChild(value = {VisEntityVirtualHashGrouper.class}, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class)
// one group per ddd of the resume (a resume with several ddds is a child of several groups): the child is a JSON text with
// these fields of the resume, as stored, and its id; it follows the resume between the main and the twin groups as the
// groups of the hashes do
@CcpEntityGrouperChild(value = {VisEntityVirtualDddGrouper.class}, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class,
		childFields = {"btc", "clt", "disponibility", "experience", "spokenLanguage", "pcd", "pj", "resumeType", "temporallyJobTime", "travel"},
		childFieldsProducers = {VisResumeSkillsFromResumeChildField.class})
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualDddGrouper.class, value = VisResumeDddCombinations.class)
// a single group counts, for each domain of notAllowedCompany, how many active resumes forbid it now: the key dropped
// by a save, the deactivation and the delete everywhere subtract 1, the reactivation adds it back
@CcpEntityGrouperChildCounter(value = {VisEntityVirtualNotAllowedCompanyCounter.class}, counterKeyClass = VisResumeNotAllowedCompanyCounterKeys.class, beforeDecoratorClass = JnGrouperChildCurrentCounterEntityBeforeWrite.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualNotAllowedCompanyCounter.class, value = VisResumeNotAllowedCompanyCounterCombinations.class)
// one group per skill the owner excluded (the items of skill whose category is excludedSkills), each counting how many
// active resumes exclude it now: the most avoided skills
@CcpEntityGrouperChildCounter(value = {VisEntityVirtualExcludedSkillCounter.class}, counterKeyClass = VisResumeExcludedSkillCounterKeys.class, beforeDecoratorClass = JnGrouperChildCurrentCounterEntityBeforeWrite.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualExcludedSkillCounter.class, value = VisResumeExcludedSkillCombinations.class)
// the moment of the insertion of the record, kept in insertionDate across the saves and the moves between the sides
@CcpEntityInsertionDate(JnInsertionDateEntity.class)
// a single group (its childName tells the entity apart) counts, for each month of insertion (yyyyMM), how many records
// inserted in that month are active now: the insertion and the reactivation add 1, the deactivation and the delete subtract 1
@CcpEntityGrouperChildCounter(value = {VisEntityVirtualInsertionMonthCounter.class}, counterKeyClass = JnInsertionYearMonthCounterKeys.class, beforeDecoratorClass = JnGrouperChildCurrentCounterEntityBeforeWrite.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualInsertionMonthCounter.class, value = VisInsertionMonthCounterCombinations.class)
public class VisEntityResume implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityResume INSTANCE = new VisEntityResume();

	private VisEntityResume() {}

	/** The entity {@code vis_resume}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;

	/**
	 * Seeds the e-mails to the owner of the resume (Portuguese and English) and their sending parameters: saved,
	 * deactivated, reactivated and deleted everywhere.
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> items = new ArrayList<>();

		String savedTemplateId = VisMessages.VisNotifyUserAboutSavedResume.class.getName();
		// a save is an insertion or a change made right now by the owner: the e-mail confirms it, says what happens next
		// (the weekly e-mail to the recruiters) and shows the resume as the recruiter will see it, with the link to fix it
		String css = VisEntityRecruiterResumes.Style.CSS.getValue();
		String oniasLinkedin = "{" + VisRecruiterResumesFields.oniasLinkedin + "}";
		String resumeFormLink = "{" + VisRecruiterResumesFields.frontendUrl + "}/meus-dados/sobre-mim";
		String resumesTable = "{" + VisRecruiterResumesFields.resumesTable + "}";
		String savedPortugueseMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Olá, aqui é o <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, do Onias.Site. Os dados do seu currículo foram salvos.</p>"
				+ "<p>A partir de agora, o seu currículo entra no e-mail que os recrutadores recebem com os currículos inseridos ou alterados nos últimos 7 dias. "
				+ "Cada vez que você atualizar os seus dados, ele volta a aparecer nesse e-mail por mais 7 dias.</p>"
				+ "<h4>Como o recrutador verá o seu currículo</h4>"
				+ "<p>Confira abaixo, com os dados que você acabou de salvar. Se algo estiver errado ou faltando, "
				+ "<a href=\"" + resumeFormLink + "\" target=\"_blank\">corrija no formulário</a>. "
				+ "Lá você também pode informar as empresas para as quais não quer que o seu currículo seja enviado.</p>"
				+ resumesTable
				+ JnEmailFooter.portuguese.getValue()
				+ "</body></html>";
		String savedEnglishMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Hello, this is <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, from Onias.Site. The data of your resume was saved.</p>"
				+ "<p>From now on, your resume is part of the e-mail the recruiters receive with the resumes added or changed in the last 7 days. "
				+ "Every time you update your data, it shows up in that e-mail for 7 more days.</p>"
				+ "<h4>How the recruiter will see your resume</h4>"
				+ "<p>Check it below, with the data you have just saved. If anything is wrong or missing, "
				+ "<a href=\"" + resumeFormLink + "\" target=\"_blank\">fix it in the form</a>. "
				+ "There you can also list the companies you do not want your resume to be sent to.</p>"
				+ resumesTable
				+ JnEmailFooter.english.getValue()
				+ "</body></html>";
		String oniasLinkedinAddress = VisEntityRecruiterResumes.Parameters.ONIAS_LINKEDIN.getValue();
		String frontendUrl = VisEntityRecruiterResumes.Parameters.FRONTEND_URL.getValue();
		CcpJsonRepresentation savedMoreParameters = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.oniasLinkedin, oniasLinkedinAddress)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
		List<CcpBulkItem> savedItems = this.getEmailTemplateAndParameters(savedTemplateId,
				"O seu currículo foi salvo",
				savedPortugueseMessage,
				"Your resume was saved",
				savedEnglishMessage,
				savedMoreParameters);
		items.addAll(savedItems);

		// the deactivation shows the same preview of the save: the candidate sees what the recruiters stop seeing
		String deactivatedTemplateId = VisMessages.VisNotifyUserAboutDeactivatedResume.class.getName();
		String deactivatedPortugueseMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Olá, aqui é o <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, do Onias.Site. Você inativou seus dados.</p>"
				+ "<p>O seu currículo deixou de ser visto pelos recrutadores. A partir de agora, ele não entra mais no e-mail que os recrutadores recebem "
				+ "com os currículos inseridos ou alterados nos últimos 7 dias.</p>"
				+ "<p>Os seus dados continuam guardados. Quando quiser voltar a ser visto, "
				+ "<a href=\"" + resumeFormLink + "\" target=\"_blank\">reative o seu currículo</a>: "
				+ "ele volta a aparecer para os recrutadores com os mesmos dados.</p>"
				+ "<h4>Como os recrutadores viam o seu currículo</h4>"
				+ "<p>Confira abaixo, com os dados guardados do seu currículo.</p>"
				+ resumesTable
				+ JnEmailFooter.portuguese.getValue()
				+ "</body></html>";
		String deactivatedEnglishMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Hello, this is <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, from Onias.Site. You deactivated your data.</p>"
				+ "<p>Your resume is no longer seen by the recruiters. From now on, it is no longer part of the e-mail the recruiters receive "
				+ "with the resumes added or changed in the last 7 days.</p>"
				+ "<p>Your data is still kept. Whenever you want to be seen again, "
				+ "<a href=\"" + resumeFormLink + "\" target=\"_blank\">reactivate your resume</a>: "
				+ "it shows up to the recruiters again with the same data.</p>"
				+ "<h4>How the recruiters saw your resume</h4>"
				+ "<p>Check it below, with the kept data of your resume.</p>"
				+ resumesTable
				+ JnEmailFooter.english.getValue()
				+ "</body></html>";
		List<CcpBulkItem> deactivatedItems = this.getEmailTemplateAndParameters(deactivatedTemplateId,
				"O seu currículo foi inativado",
				deactivatedPortugueseMessage,
				"Your resume was deactivated",
				deactivatedEnglishMessage,
				savedMoreParameters);
		items.addAll(deactivatedItems);

		// the reactivation shows the same preview of the save: the candidate sees the resume as the recruiter will see it
		String reactivatedTemplateId = VisMessages.VisNotifyUserAboutReactivatedResume.class.getName();
		String reactivatedPortugueseMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Olá, aqui é o <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, do Onias.Site. Você reativou seus dados.</p>"
				+ "<p>O seu currículo voltou a aparecer para os recrutadores. Ele entra no e-mail que os recrutadores recebem com os currículos inseridos ou alterados nos últimos 7 dias: "
				+ "cada vez que você atualizar os seus dados, ele aparece nesse e-mail por mais 7 dias.</p>"
				+ "<h4>Como o recrutador verá o seu currículo</h4>"
				+ "<p>Confira abaixo, com os dados do seu currículo. Se algo estiver errado ou faltando, "
				+ "<a href=\"" + resumeFormLink + "\" target=\"_blank\">corrija no formulário</a>. "
				+ "Lá você também pode informar as empresas para as quais não quer que o seu currículo seja enviado.</p>"
				+ resumesTable
				+ JnEmailFooter.portuguese.getValue()
				+ "</body></html>";
		String reactivatedEnglishMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Hello, this is <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, from Onias.Site. You reactivated your data.</p>"
				+ "<p>Your resume shows up to the recruiters again. It is part of the e-mail the recruiters receive with the resumes added or changed in the last 7 days: "
				+ "every time you update your data, it shows up in that e-mail for 7 more days.</p>"
				+ "<h4>How the recruiter will see your resume</h4>"
				+ "<p>Check it below, with the data of your resume. If anything is wrong or missing, "
				+ "<a href=\"" + resumeFormLink + "\" target=\"_blank\">fix it in the form</a>. "
				+ "There you can also list the companies you do not want your resume to be sent to.</p>"
				+ resumesTable
				+ JnEmailFooter.english.getValue()
				+ "</body></html>";
		List<CcpBulkItem> reactivatedItems = this.getEmailTemplateAndParameters(reactivatedTemplateId,
				"O seu currículo foi reativado",
				reactivatedPortugueseMessage,
				"Your resume was reactivated",
				reactivatedEnglishMessage,
				savedMoreParameters);
		items.addAll(reactivatedItems);

		String deletedTemplateId = VisMessages.VisNotifyUserAboutDeletedResume.class.getName();
		List<CcpBulkItem> deletedItems = this.getEmailTemplateAndParameters(deletedTemplateId,
				"Os dados do seu currículo foram excluídos",
				"<html><head>" + css + "</head><body>"
						+ "<p>Olá, aqui é o <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, do Onias.Site. "
						+ "Todos os dados do seu currículo foram excluídos do Onias.Site de forma permanente.</p>"
						+ JnEmailFooter.portuguese.getValue()
						+ "</body></html>",
				"Your resume data was deleted",
				"<html><head>" + css + "</head><body>"
						+ "<p>Hello, this is <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>, from Onias.Site. "
						+ "Every data of your resume was permanently deleted from Onias.Site.</p>"
						+ JnEmailFooter.english.getValue()
						+ "</body></html>",
				savedMoreParameters);
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
		List<CcpBulkItem> templateAndParameters = this.getEmailTemplateAndParameters(templateId, portugueseSubject, portugueseMessage, englishSubject, englishMessage, CcpOtherConstants.EMPTY_JSON);
		return templateAndParameters;
	}

	/**
	 * The same of {@link #getEmailTemplateAndParameters(String, String, String, String, String)}, with the
	 * {@code moreParameters} of the sending (the addresses of the environment), when not empty.
	 * @param templateId the class name of the message
	 * @param portugueseSubject the subject in Portuguese
	 * @param portugueseMessage the message in Portuguese
	 * @param englishSubject the subject in English
	 * @param englishMessage the message in English
	 * @param moreParameters the values of the environment merged into the message
	 * @return the bulk items of the templates and of the parameters
	 */
	private List<CcpBulkItem> getEmailTemplateAndParameters(String templateId, String portugueseSubject, String portugueseMessage, String englishSubject, String englishMessage, CcpJsonRepresentation moreParameters) {
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
				.put(JnJsonCommonsFields.sender, "curriculos@onias.site");
		CcpJsonRepresentation parametersWithSubjectType = parametersWithSender
				.put(JnJsonCommonsFields.subjectType, templateId);
		CcpJsonRepresentation parametersWithTemplateId = parametersWithSubjectType
				.put(JnJsonCommonsFields.templateId, templateId);
		boolean noMoreParameters = moreParameters.isEmpty();
		CcpJsonRepresentation parameters = noMoreParameters ? parametersWithTemplateId : parametersWithTemplateId.put(JnJsonCommonsFields.moreParameters, moreParameters);

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
			},
			customJsonValidators = {VisJsonValidatorHomeOfficeAlone.class}
	)
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
		 * The {@code hash} field: list, validated as in {@code VisJsonCommonsFields}; filled on every save by the decorator
		 * of {@code @CcpEntityGrouperChild}, it holds the id of every search the resume satisfies: the groups the resume is
		 * in, one per combination of {@code VisResumeHashCombinations}.
		 */
		@CcpJsonFieldValidatorArray
		@CcpStoredParentIdsField(value = VisResumeHashCombinations.class, parent = VisEntityVirtualHashGrouper.class)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
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

		/** The {@code linkedinAddress} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		linkedinAddress,

		/** The {@code negotiableClaim} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		negotiableClaim,

		/**
		 * The {@code notAllowedCompany} field: list, text; each item is the domain of a consultancy the resume must not
		 * reach ({@code consultoria.com.br}): labels of letters, digits and hyphens (no hyphen at either end) separated by
		 * dots, ending in a top level domain of letters only (since 2026-11-11; before it took any text, such as company
		 * names).
		 */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 4, maxLength = 50, regexValidation = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$")
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

		/**
		 * The {@code insertionDate} field: the moment the record was inserted, kept by {@code @CcpEntityInsertionDate}
		 * (the value of the input is ignored), validated as in {@code JnJsonCommonsFields}.
		 */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		insertionDate,
		;
	}	
}

