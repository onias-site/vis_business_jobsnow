package com.vis.entities;

import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.business.telegram.VisTelegramSystemMessages;
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;

/**
 * The positions of a month, region and seniority: the filters of the screen Estatísticas › Skills. Its id names the group
 * of {@code jn_grouper} whose children are the skills of that month ({@link VisEntityStatisSkillLineChart}, each one with
 * its total), the items of the accordion and the slices of the pie chart. Its {@code total} counts each position once, so
 * it is not the sum of the slices: a position with three skills is one here and one in each of the three slices. The past
 * months stay on purpose, and the month is in the primary key, so the entity is not disposable. Written by
 * {@code VisBusinessRecalculateSkillStatistics}.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityStatisSkillPieChart.Fields.class)
public class VisEntityStatisSkillPieChart implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityStatisSkillPieChart INSTANCE = new VisEntityStatisSkillPieChart();

	private VisEntityStatisSkillPieChart() {}

	/** The entity {@code vis_statis_skill_pie_chart}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityStatisSkillPieChart.class).entityInstance;

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

		/** The {@code yearMonth} field: part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		yearMonth,

		/** The {@code total} field: required, how many positions there are, each one counted once. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		total,
		;
	}

	/**
	 * The links of the Telegram groups shown on the screen of this chart.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> seeds = VisTelegramSystemMessages.getSeeds();
		return seeds;
	}
}
