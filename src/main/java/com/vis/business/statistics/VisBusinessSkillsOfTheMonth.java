package com.vis.business.statistics;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeCustom;
import com.jn.entities.JnEntityGrouper;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityStatisSkillLineChart;
import com.vis.entities.VisEntityStatisSkillPieChart;
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;
import com.vis.json.fields.validation.VisYearMonthFieldType;

/**
 * The skills of the positions of a month, region and seniority, each one as a JSON text with its {@code skill} and
 * {@code total}, and {@code total}: how many positions, each one counted once. Two reads by id (OQL): the group of the
 * pie chart and, when it exists, the pie chart. With no position, {@code total} is 0 and the list is empty.
 */
public class VisBusinessSkillsOfTheMonth implements CcpBusiness {

	/** The input and the output of this business, with the validation rules of the input. */
	public static enum Fields implements CcpJsonFieldName {
		/** The month, as {@code yyyyMM}: from 2015 up to the current year, months 01 to 12. */
		@CcpJsonFieldTypeCustom(VisYearMonthFieldType.class)
		@CcpJsonFieldValidatorRequired
		yearMonth,

		/** The seniority. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		@CcpJsonFieldValidatorRequired
		seniority,

		/** The region. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		@CcpJsonFieldValidatorRequired
		ddd,

		/** How many positions the combination has. */
		total,

		/** The skills of the combination. */
		skills,
	}

	/** The single instance. */
	public static final VisBusinessSkillsOfTheMonth INSTANCE = new VisBusinessSkillsOfTheMonth();

	private VisBusinessSkillsOfTheMonth() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the skills.
	 * @param json the month, the region and the seniority
	 * @return {@code total} and {@code skills}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Integer ddd = json.getAsIntegerNumber(Fields.ddd);
		CcpJsonRepresentation key = json.getJsonPiece(Fields.yearMonth, Fields.seniority).put(Fields.ddd, ddd);
		String pieChartId = VisEntityStatisSkillPieChart.ENTITY.calculateId(key);
		CcpEntityMetaData pieChartDetails = VisEntityStatisSkillPieChart.ENTITY.getEntityMetaData();
		CcpEntityMetaData lineChartDetails = VisEntityStatisSkillLineChart.ENTITY.getEntityMetaData();
		CcpJsonRepresentation group = JnGroupers.getGroup(false, pieChartDetails.entityName, lineChartDetails.entityName, pieChartId);
		List<String> skills = group.getAsStringList(JnEntityGrouper.Fields.child);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.skills, skills);
		boolean noPositions = skills.isEmpty();

		if(noPositions) {
			return response.put(Fields.total, 0);
		}

		CcpJsonRepresentation pieChart = VisEntityStatisSkillPieChart.ENTITY.getOneById(key);
		Integer total = pieChart.getAsIntegerNumber(VisEntityStatisSkillPieChart.Fields.total);
		return response.put(Fields.total, total);
	}
}
