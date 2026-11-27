package com.vis.entities;

import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpExceptionFlow;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpAfterSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.business.job.posting.VisBusinessFindTheMostCoveredSkills;
import com.vis.business.job.posting.VisBusinessFindTheSkillsOfTheJobPosting;
import com.vis.business.job.posting.VisBusinessLogTheRefusedJobPosting;
import com.vis.business.job.posting.VisBusinessRegisterTheRecruitersOfTheJobPosting;
import com.vis.business.job.posting.VisBusinessReplyTheJobPostingWithItsResumes;
import com.vis.business.job.posting.VisBusinessSaveTheDescriptionOfTheJobPosting;
import com.vis.business.job.posting.VisBusinessSaveTheResumeFilterOfTheJobPosting;
import com.vis.business.job.posting.VisBusinessSendTheResumesOfTheJobPosting;
import com.vis.business.job.posting.VisErrorJobPostingDescriptionNotSaved;
import com.vis.business.job.posting.VisErrorJobPostingWithoutSkills;
import com.vis.business.job.posting.VisJobPostingReplyTexts;

/**
 * A job posting written in a group of Telegram where the user bot is (since 2026-11-27), one per person who posts it
 * ({@code chatId}) a day. The writing is synchronous. Before the save run the same six businesses of
 * {@code vis_job_posting}, in the same order and with the same refusals:
 * <ol>
 * <li>the valid e-mails of the text registered as recruiters ({@link VisBusinessRegisterTheRecruitersOfTheJobPosting});</li>
 * <li>the skills of the text; no skill, the job posting is refused ({@link VisBusinessFindTheSkillsOfTheJobPosting},
 * {@link VisErrorJobPostingWithoutSkills});</li>
 * <li>the largest set of those skills that some resume has ({@link VisBusinessFindTheMostCoveredSkills});</li>
 * <li>the search of resumes of {@code curriculos/buscar} over that set, its count, averages and link
 * ({@link VisBusinessSaveTheResumeFilterOfTheJobPosting});</li>
 * <li>the text shared with the filters of that search; filters that already have a text, or no filters, refuse the job
 * posting ({@link VisBusinessSaveTheDescriptionOfTheJobPosting}, {@link VisErrorJobPostingDescriptionNotSaved});</li>
 * <li>the e-mail of the resumes found to the recruiters of the text ({@link VisBusinessSendTheResumesOfTheJobPosting}).</li>
 * </ol>
 * A refusal is only logged ({@link VisBusinessLogTheRefusedJobPosting}) and cancels the save: the message of the group is
 * ignored. After the save the user bot answers the message of the job posting in the group with the resumes found
 * ({@link VisBusinessReplyTheJobPostingWithItsResumes}); no resume found, no answer. The records are discarded day by day.
 */
@CcpEntityCache(3600)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityInstantMessagePositionReply.Fields.class)
// the same six businesses of vis_job_posting, with the same refusals, which only log and cancel the save
@CcpBeforeSaveFromMainEntity(execute = {VisBusinessRegisterTheRecruitersOfTheJobPosting.class, VisBusinessFindTheSkillsOfTheJobPosting.class, VisBusinessFindTheMostCoveredSkills.class,
		VisBusinessSaveTheResumeFilterOfTheJobPosting.class, VisBusinessSaveTheDescriptionOfTheJobPosting.class, VisBusinessSendTheResumesOfTheJobPosting.class},
		operationHandlers = {@CcpExceptionFlow(whenThrowing = VisErrorJobPostingWithoutSkills.class, thenExecute = {VisBusinessLogTheRefusedJobPosting.class}),
				@CcpExceptionFlow(whenThrowing = VisErrorJobPostingDescriptionNotSaved.class, thenExecute = {VisBusinessLogTheRefusedJobPosting.class})})
@CcpAfterSaveFromMainEntity(execute = {VisBusinessReplyTheJobPostingWithItsResumes.class}, operationHandlers = {})
public class VisEntityInstantMessagePositionReply implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityInstantMessagePositionReply INSTANCE = new VisEntityInstantMessagePositionReply();

	private VisEntityInstantMessagePositionReply() {}

	/** The entity {@code vis_instant_message_position_reply}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityInstantMessagePositionReply.class).entityInstance;

	/** Seeds the text of the answer in the group ({@code jn_system_message}). */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> seeds = VisJobPostingReplyTexts.getSeeds();
		return seeds;
	}

	/**
	 * The fields of the entity, with their validation rules. The fields written by the businesses before the save have the
	 * names of the fields of {@code vis_job_posting}, which those businesses write.
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code chatId} field: the primary key, the id of the person who posts the job posting in the group of Telegram. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		chatId,

		/** The {@code groupChatId} field: required, the chat of the group where the job posting was written, where the answer goes. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumber
		groupChatId,

		/** The {@code replyTo} field: required, the id of the message of the job posting in the group, which the answer answers. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned
		replyTo,

		/** The {@code description} field: required, the text of the job posting, from 100 to 5,000 characters. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 100, maxLength = 5000)
		description,

		/**
		 * The {@code linkToShowResumes} field: the address of the screen {@code curriculos/buscar} with the filters of the
		 * resumes found and the text of the job posting.
		 */
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 500)
		linkToShowResumes,

		/** The {@code resumesCount} field: how many resumes the search of the filters found. */
		@CcpJsonFieldTypeNumberUnsigned
		resumesCount,

		/** The {@code avgExperience} field: the average years of experience of the resumes found that informed it. */
		@CcpJsonFieldTypeNumber
		avgExperience,

		/** The {@code avgClt} field: the average CLT claim of the resumes found that informed it. */
		@CcpJsonFieldTypeNumber
		avgClt,

		/** The {@code avgPj} field: the average PJ claim of the resumes found that informed it. */
		@CcpJsonFieldTypeNumber
		avgPj,
		;
	}
}
