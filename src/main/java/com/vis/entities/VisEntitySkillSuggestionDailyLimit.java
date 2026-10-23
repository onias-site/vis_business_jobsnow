package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * The candidate ({@code email}, kept as hash) suggested a new skill in the last 24 hours: one record per candidate,
 * written when a suggestion is saved and valid for one day from then. While it lasts, a new suggestion of any skill is refused
 * with {@code VisProcessStatusSuggestSkill.dailyLimitReached} (429). Each suggestion emails the candidate and opens a
 * ticket to the support operator, so without this limit an automation could drain the email quota and load the backend.
 * <p>
 * The block lasts 24 hours from the suggestion, not until midnight: after the turn of the day the document of the
 * period no longer matches, but its copy in {@code jn_disposable_record} expires one day after it was written, and
 * {@code JnDisposableEntity.exists} still accepts it until then.
 * The refusals that send nothing (known skill, repeated suggestion, rejected or reviewed skill) are checked before
 * this one and do not spend the day's suggestion.
 * <p>
 * Not cached: the check is a single read by id, and without cache the test scripts can free the day by deleting the
 * record straight from the database.
 */
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntitySkillSuggestionDailyLimit.Fields.class)
public class VisEntitySkillSuggestionDailyLimit implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntitySkillSuggestionDailyLimit INSTANCE = new VisEntitySkillSuggestionDailyLimit();

	private VisEntitySkillSuggestionDailyLimit() {}

	/** The entity {@code vis_skill_suggestion_daily_limit}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillSuggestionDailyLimit.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code email} field: the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,
		;
	}
}
