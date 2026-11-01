package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityStatisSkill;

/**
 * The parent of a point of the line chart ({@code vis_statis_skill_line_chart}) among the skills of a region and
 * seniority: a single combination, whose id in {@link VisEntityStatisSkill} names the group of the months of the skill.
 */
public class VisStatisSkillCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisStatisSkillCombinations INSTANCE = new VisStatisSkillCombinations();

	private VisStatisSkillCombinations() {}

	/**
	 * The only combination of the point: its region, seniority and skill.
	 * @param json the point
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = json.getJsonPiece(VisEntityStatisSkill.Fields.values());
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
