package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityViewedResumesOfTheRecruiter;

/**
 * The parent of a view of a resume ({@code vis_viewed_resume}) in the group of every view of the recruiter (since
 * 2026-11-15): a single combination, the e-mail of the recruiter, whose id in {@code VisEntityViewedResumesOfTheRecruiter}
 * names that group, the same in every month.
 */
public class VisViewedResumeRecruiterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisViewedResumeRecruiterCombinations INSTANCE = new VisViewedResumeRecruiterCombinations();

	private VisViewedResumeRecruiterCombinations() {}

	/**
	 * The only combination of the view: its e-mail.
	 * @param json the view
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = json.getJsonPiece(VisEntityViewedResumesOfTheRecruiter.Fields.email);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
