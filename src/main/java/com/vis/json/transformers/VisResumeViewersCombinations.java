package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResumeViewersOfTheMonth;

/**
 * The parent of the viewers of a resume in a month ({@code vis_resume_viewers_of_the_month}): a single combination, the
 * e-mail of the resume, whose id in {@code VisEntityResume} is the id of the resume.
 */
public class VisResumeViewersCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisResumeViewersCombinations INSTANCE = new VisResumeViewersCombinations();

	private VisResumeViewersCombinations() {}

	/**
	 * The only combination of the viewers of the month: the e-mail of the resume.
	 * @param json the viewers of the month
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = json.getJsonPiece(VisEntityResumeViewersOfTheMonth.Fields.email);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
