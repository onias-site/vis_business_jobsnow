package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChildCounter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonEntityBeforeWrite;
import com.jn.entities.decorators.engine.JnGrouperChildCounterEntityBeforeWrite;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerLinkedinHash;
import com.vis.json.transformers.VisResumeViewsOfTheMonthCombinations;
import com.vis.json.transformers.VisViewedResumeMonthCombinations;
import com.vis.json.transformers.VisViewedResumeRecruiterDomain;

/**
 * The resumes viewed by each recruiter: one record per resume ({@code linkedin}) and recruiter ({@code email}). Both
 * keys are kept only as SHA-1 hashes (LGPD): this system avoids writing personal data in a readable form, so the
 * recruiter refers to the resume by an {@code alias} of their own choosing.
 * <p>
 * Every save also keeps the view in the group of the recruiter in the current month ({@code jn_grouper}, parent
 * {@code vis_viewed_resumes_of_the_month}): one JSON text per resume, with the {@code linkedin} as it came (readable),
 * the {@code alias} and the {@code date} of the save, in place of the earlier view of the same resume in that month; the
 * months before are kept. The deletes leave the groups as they are (they are a history).
 * <p>
 * Every save also counts the view among the views of the month of the resume, for the screen "Quem me viu" of the
 * candidate ({@code jn_grouper}, parent {@code vis_resume_views_of_the_month}, one per e-mail that uses the LinkedIn
 * address): a single JSON with one counter per domain of recruiter, where each recruiter counts once per month (the
 * first save of the view in the month).
 * <p>
 * Both relationships are oneToMany (a view has one recruiter and one resume), so the parents are declared by
 * {@code @CcpEntityGrouperParentCombinations} and no field keeps their ids.
 */
@CcpEntityCache(3600)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsValidator(VisEntityViewedResume.Fields.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
// the views of the month of the recruiter, as JSON texts with the LinkedIn as it came, and the views of the month of the
// resume, counted by domain: both decorators stay before the field transformers, which store the LinkedIn and the
// e-mail of this entity as their SHA-1
@CcpEntityGrouperChild(value = {VisEntityViewedResumesOfTheMonth.class}, beforeDecoratorClass = JnGrouperChildAsJsonEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildAsJsonEntityAfterWrite.class)
@CcpEntityGrouperChildCounter(value = {VisEntityResumeViewsOfTheMonth.class}, counterKeyClass = VisViewedResumeRecruiterDomain.class, beforeDecoratorClass = JnGrouperChildCounterEntityBeforeWrite.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityViewedResumesOfTheMonth.class, value = VisViewedResumeMonthCombinations.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityResumeViewsOfTheMonth.class, value = VisResumeViewsOfTheMonthCombinations.class)
public class VisEntityViewedResume implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityViewedResume INSTANCE = new VisEntityViewedResume();

	private VisEntityViewedResume() {}

	/** The entity {@code vis_viewed_resume}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityViewedResume.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code linkedin} field: part of the primary key, the LinkedIn address of the viewed resume; stored as its
		 * SHA-1 hash by {@code VisJsonTransformerLinkedinHash}.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(regexValidation = "^https://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-_%]*[a-zA-Z0-9_%]/?$")
		@CcpEntityFieldTransformer(VisJsonTransformerLinkedinHash.class)
		linkedin,

		/**
		 * The {@code email} field: part of the primary key, the e-mail of the recruiter who viewed the resume, validated
		 * as in {@code JnJsonCommonsFields}; stored as its SHA-1 hash by the {@code email} transformer of
		 * {@code JnJsonTransformersFieldsEntityDefault}.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/**
		 * The {@code alias} field: required, text of 5 to 200 characters, the nickname the recruiter gave to the resume,
		 * since the resume itself is not kept in a readable form.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 5, maxLength = 200)
		alias,

		/**
		 * The {@code date} field: text, the date and time of the last save ({@code dd/MM/yyyy HH:mm:ss}), filled by the
		 * decorator of {@code @CcpEntityGrouperChild}.
		 */
		@CcpJsonFieldTypeString(exactLength = 19)
		date,
	}
}
