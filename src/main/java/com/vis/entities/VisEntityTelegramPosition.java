package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpStoredParentIdsField;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.decorators.engine.JnGrouperChildEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;
import com.vis.json.transformers.VisTelegramPositionLineChartCombinations;

/**
 * The positions announced in the Telegram groups, one per contact, set of skills and day of the message: the same
 * position published again in the same day is the same record, and in another day it is another one. Each day is a
 * period ({@code @CcpEntityDisposable} daily, written through {@code disposablePeriod} with the date of the message) and
 * every period stays ({@code removeCompletely = false}): the HTML files of the load are deleted, so this index is the only
 * source of the statistics of skills and of the ranking of {@link VisEntitySkill}.
 * <p>
 * Each position is a child, in {@code JnEntityGrouper}, of the group of each skill it has in its month, region and
 * seniority ({@link VisEntityStatisSkillLineChart}). The relationship is manyToMany, its parent ids kept in
 * {@code statisSkillLineChart}. The load writes by bulk, which the decorator of {@code @CcpEntityGrouperChild} does not
 * see, so {@code VisBusinessRecalculateSkillStatistics} writes these groups from the index.
 */
@CcpEntityCache(3600)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily, removeCompletely = false)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityTelegramPosition.Fields.class)
@CcpEntityGrouperChild(value = {VisEntityStatisSkillLineChart.class}, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class)
public class VisEntityTelegramPosition implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityTelegramPosition INSTANCE = new VisEntityTelegramPosition();

	private VisEntityTelegramPosition() {}

	/** The entity {@code vis_telegram_position}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityTelegramPosition.class).entityInstance;

	/**
	 * The setup leaves the positions alone: the HTML files of the load are deleted, so they cannot be read again.
	 * @return {@code true}
	 */
	public boolean keepsItsRecordsOnTheSetup() {
		return true;
	}

	/** The texts of the entity. */
	public static enum Constants implements CcpJsonFieldName {
		/** What joins the skills of a position in the {@code skill} field. */
		SKILLS_SEPARATOR { public String getValue() { return "_"; } },
	}

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code contact} field: part of the primary key, the e-mail, link, WhatsApp or phone of the position. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 2000)
		contact,

		/**
		 * The {@code skill} field: part of the primary key, the skills of the position in upper case, with no repetition, in
		 * alphabetical order and joined by {@link Constants#SKILLS_SEPARATOR}.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 5000)
		skill,

		/** The {@code seniority} field: required, told by the text of the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		seniority,

		/** The {@code ddd} field: required, told by the text of the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		ddd,

		/** The {@code yearMonth} field: required, the month of the message. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		yearMonth,

		/** The {@code statisSkillLineChart} field: list; the parent id of each group the position is in, one per skill. */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		@CcpStoredParentIdsField(value = VisTelegramPositionLineChartCombinations.class, parent = VisEntityStatisSkillLineChart.class)
		statisSkillLineChart,
		;
	}
}
