package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityStatisSkillPieChart;

/**
 * The parent of a point of the line chart ({@code vis_statis_skill_line_chart}) among the months: a single combination,
 * whose id in {@link VisEntityStatisSkillPieChart} names the group of the skills of the month, region and seniority.
 */
public class VisStatisSkillPieChartCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisStatisSkillPieChartCombinations INSTANCE = new VisStatisSkillPieChartCombinations();

	private VisStatisSkillPieChartCombinations() {}

	/**
	 * The only combination of the point: its region, seniority and month.
	 * @param json the point
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = json.getJsonPiece(VisEntityStatisSkillPieChart.Fields.ddd, VisEntityStatisSkillPieChart.Fields.seniority, VisEntityStatisSkillPieChart.Fields.yearMonth);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
