package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityStatisSkillLineChart;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * The parents of a position of Telegram ({@code vis_telegram_position}): one combination per skill it has, whose id in
 * {@link VisEntityStatisSkillLineChart} names the group of the positions with that skill in the month, region and
 * seniority of the position.
 */
public class VisTelegramPositionLineChartCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisTelegramPositionLineChartCombinations INSTANCE = new VisTelegramPositionLineChartCombinations();

	private VisTelegramPositionLineChartCombinations() {}

	/**
	 * The combination of each skill of the position.
	 * @param json the position
	 * @return the combinations
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation position = json.getJsonPiece(VisEntityStatisSkillLineChart.Fields.ddd, VisEntityStatisSkillLineChart.Fields.seniority, VisEntityStatisSkillLineChart.Fields.yearMonth);
		String joinedSkills = json.getAsString(VisEntityTelegramPosition.Fields.skill);
		String skillsSeparator = VisEntityTelegramPosition.Constants.SKILLS_SEPARATOR.getValue();
		String[] skills = joinedSkills.split(skillsSeparator);
		List<String> skillsOfThePosition = Arrays.asList(skills);
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String skill : skillsOfThePosition) {
			CcpJsonRepresentation combination = position.put(VisEntityStatisSkillLineChart.Fields.skill, skill);
			combinations.add(combination);
		}
		return combinations;
	}
}
