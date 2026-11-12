package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualNotAllowedCompanyCounter;

/**
 * Produces the group of {@code VisEntityVirtualNotAllowedCompanyCounter} every resume is counted in: always the same,
 * the only group of the counters of the forbidden consultancies.
 */
public class VisResumeNotAllowedCompanyCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeNotAllowedCompanyCounterCombinations INSTANCE = new VisResumeNotAllowedCompanyCounterCombinations();

	private VisResumeNotAllowedCompanyCounterCombinations() {}

	/**
	 * The combination of the only group, whatever the resume.
	 * @param json the resume
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualNotAllowedCompanyCounter.getCombination();
		return Arrays.asList(combination);
	}
}
