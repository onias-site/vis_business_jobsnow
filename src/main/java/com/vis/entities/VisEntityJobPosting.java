package com.vis.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.decorators.engine.JnGrouperChildEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.business.bots.job.posting.VisSupportSaveJobPostingBotConfiguration;
import com.vis.business.job.posting.VisBusinessFindTheMostCoveredSkills;
import com.vis.business.job.posting.VisBusinessFindTheSkillsOfTheJobPosting;
import com.vis.business.job.posting.VisBusinessSaveTheResumeFilterOfTheJobPosting;
import com.vis.business.job.posting.VisJobPostingPublishingTexts;
import com.vis.json.transformers.VisJobPostingHourCombinations;
import com.vis.json.transformers.VisJsonTransformerJobPostingId;

/**
 * The text of a job posting saved by the support operator with {@code /saveJobPosting} (since 2026-11-26), with the
 * resumes that best answer it. The writing is asynchronous: the command of the bot only publishes the text, and the
 * queue of the entity runs, before the save, the businesses that find the resumes:
 * <ol>
 * <li>the skills of the text ({@code skills/fromText}), the main ones and their parents
 * ({@link VisBusinessFindTheSkillsOfTheJobPosting});</li>
 * <li>the groups of those skills in {@code jn_grouper} ({@code vis_virtual_skill_grouper}, one read by id of each), crossed
 * to find the largest set of them that some resume has ({@link VisBusinessFindTheMostCoveredSkills});</li>
 * <li>the search of resumes of the screen {@code curriculos/buscar} with those skills as required, its text shared under
 * the e-mail {@code curriculos@onias.site}, and its count and averages kept in the record
 * ({@link VisBusinessSaveTheResumeFilterOfTheJobPosting}).</li>
 * </ol>
 * The records are discarded day by day. Each one is also the child (its text and its id) of one group of
 * {@link VisEntityVirtualJobPostingHour}: the day of the saving and one of the hours still to come in it, from the next
 * hour to 23, chosen by the millisecond of the saving (23 when saved at 23h).
 */
@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityJobPosting.Fields.class)
// the skills of the text, the largest set of them that some resume has and the search of resumes over that set
@CcpBeforeSaveFromMainEntity(execute = {VisBusinessFindTheSkillsOfTheJobPosting.class, VisBusinessFindTheMostCoveredSkills.class, VisBusinessSaveTheResumeFilterOfTheJobPosting.class}, operationHandlers = {})
// one group per day and hour still to come in it (the next hour to 23, by the millisecond): the child is a JSON text with the text of the job
// posting and its id
@CcpEntityGrouperChild(value = {VisEntityVirtualJobPostingHour.class}, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class,
		childFields = {"description"})
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualJobPostingHour.class, value = VisJobPostingHourCombinations.class)
public class VisEntityJobPosting implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityJobPosting INSTANCE = new VisEntityJobPosting();

	private VisEntityJobPosting() {}

	/** The entity {@code vis_job_posting}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityJobPosting.class).entityInstance;

	/** Seeds the configuration of {@code /saveJobPosting} in the bot entities of jn and the texts of its hourly publishing in the groups. */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> botSeeds = VisSupportSaveJobPostingBotConfiguration.getSeeds();
		List<CcpBulkItem> publishingTexts = VisJobPostingPublishingTexts.getSeeds();
		List<CcpBulkItem> seeds = new ArrayList<>(botSeeds);
		seeds.addAll(publishingTexts);
		return seeds;
	}

	/** The fields of the entity, with their validation rules. */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code postId} field: the primary key, the SHA-1 of the text without the spaces around it and in upper case
		 * ({@code VisJsonTransformerJobPostingId}); up to 40 hexadecimal characters, with the sign of the hash of the Java
		 * when negative ({@code CcpHashDecorator}: 41 characters).
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 41, regexValidation = "^-?[0-9a-fA-F]+$")
		@CcpEntityFieldTransformer(VisJsonTransformerJobPostingId.class)
		postId,

		/** The {@code description} field: required, the text of the job posting, from 100 to 5,000 characters. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 100, maxLength = 5000)
		description,

		/**
		 * The {@code linkToShowResumes} field: the address of the screen {@code curriculos/buscar} with the filters of the
		 * resumes found and the text of the job posting; absent when no resume has any skill of the text.
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

		/**
		 * The {@code timestamp} field: the millisecond of the saving, set by the command of the bot; it gives the group of
		 * the record ({@code VisJobPostingHourCombinations}). A number, not the unsigned integer of
		 * {@code JnJsonCommonsFields}: the record crosses the queue of the entity as JSON, where the millisecond becomes a
		 * double written in scientific notation ({@code 1.76E12}), which the unsigned integer refuses; a double holds it
		 * exactly, and the index keeps it as {@code long}.
		 */
		@CcpJsonFieldTypeNumber
		timestamp,
		;
	}
}
