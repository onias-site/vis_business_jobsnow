package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityViewedResumesOfTheMonth;

/**
 * The parent of a view of a resume ({@code vis_viewed_resume}): a single combination, the e-mail of the recruiter, whose
 * id in {@code VisEntityViewedResumesOfTheMonth} names the group of the views of the recruiter in the current month.
 */
public class VisViewedResumeMonthCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisViewedResumeMonthCombinations INSTANCE = new VisViewedResumeMonthCombinations();

	private VisViewedResumeMonthCombinations() {}

	/**
	 * The only combination of the view: its e-mail.
	 * @param json the view
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = json.getJsonPiece(VisEntityViewedResumesOfTheMonth.Fields.email);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
