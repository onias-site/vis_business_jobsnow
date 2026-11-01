package com.vis.business.statistics;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.JnEntityGrouper;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityStatisSkill;
import com.vis.entities.VisEntityStatisSkillLineChart;
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;

/**
 * The months of a skill in a region and seniority, each one as a JSON text with its {@code yearMonth} and {@code total}:
 * the points of the line chart, one read by id (OQL). With no position, the list is empty.
 */
public class VisBusinessMonthsOfTheSkill implements CcpBusiness {

	/** The input and the output of this business, with the validation rules of the input. */
	public static enum Fields implements CcpJsonFieldName {
		/** The skill. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		@CcpJsonFieldValidatorRequired
		skill,

		/** The seniority. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		@CcpJsonFieldValidatorRequired
		seniority,

		/** The region. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		@CcpJsonFieldValidatorRequired
		ddd,

		/** The months of the skill. */
		months,
	}

	/** The single instance. */
	public static final VisBusinessMonthsOfTheSkill INSTANCE = new VisBusinessMonthsOfTheSkill();

	private VisBusinessMonthsOfTheSkill() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the months.
	 * @param json the skill, the region and the seniority
	 * @return {@code months}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Integer ddd = json.getAsIntegerNumber(Fields.ddd);
		CcpJsonRepresentation key = json.getJsonPiece(Fields.skill, Fields.seniority).put(Fields.ddd, ddd);
		String statisSkillId = VisEntityStatisSkill.ENTITY.calculateId(key);
		CcpEntityMetaData statisSkillDetails = VisEntityStatisSkill.ENTITY.getEntityMetaData();
		CcpEntityMetaData lineChartDetails = VisEntityStatisSkillLineChart.ENTITY.getEntityMetaData();
		CcpJsonRepresentation group = JnGroupers.getGroup(false, statisSkillDetails.entityName, lineChartDetails.entityName, statisSkillId);
		List<String> months = group.getAsStringList(JnEntityGrouper.Fields.child);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.months, months);
		return response;
	}
}
