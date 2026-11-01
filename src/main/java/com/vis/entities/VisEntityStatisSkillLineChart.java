package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;
import com.vis.json.transformers.VisStatisSkillCombinations;
import com.vis.json.transformers.VisStatisSkillPieChartCombinations;

/**
 * A point of the line chart of a skill: how many records of {@link VisEntityTelegramPosition} have the skill in a month,
 * region and seniority. Each point is a child, as a JSON text, of two groups of {@code JnEntityGrouper}, both oneToMany,
 * so the screen reads everything by id (OQL): the group of the skill in the region and seniority
 * ({@link VisEntityStatisSkill}, its months) and the group of the month, region and seniority
 * ({@link VisEntityStatisSkillPieChart}, its skills). Written by {@code VisBusinessRecalculateSkillStatistics}, by bulk,
 * which the decorator of {@code @CcpEntityGrouperChild} does not see: that process writes the groups too.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityStatisSkillLineChart.Fields.class)
@CcpEntityGrouperChild(value = {VisEntityStatisSkill.class, VisEntityStatisSkillPieChart.class}, beforeDecoratorClass = JnGrouperChildAsJsonEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildAsJsonEntityAfterWrite.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityStatisSkill.class, value = VisStatisSkillCombinations.class)
@CcpEntityGrouperParentCombinations(parent = VisEntityStatisSkillPieChart.class, value = VisStatisSkillPieChartCombinations.class)
public class VisEntityStatisSkillLineChart implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityStatisSkillLineChart INSTANCE = new VisEntityStatisSkillLineChart();

	private VisEntityStatisSkillLineChart() {}

	/** The entity {@code vis_statis_skill_line_chart}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityStatisSkillLineChart.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code ddd} field: part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		ddd,

		/** The {@code seniority} field: part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		seniority,

		/** The {@code skill} field: part of the primary key, a single skill. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		skill,

		/** The {@code yearMonth} field: part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		yearMonth,

		/** The {@code total} field: required, the records of the position with the skill. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		total,
		;
	}
}
