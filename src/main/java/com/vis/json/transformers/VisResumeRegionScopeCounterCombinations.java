package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualResumeRegionScopeCounter;

/**
 * Produces the group of {@code VisEntityVirtualResumeRegionScopeCounter} every resume is counted in: always the same, the
 * only group of the reach of the ddds of the resumes.
 */
public class VisResumeRegionScopeCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeRegionScopeCounterCombinations INSTANCE = new VisResumeRegionScopeCounterCombinations();

	private VisResumeRegionScopeCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualResumeRegionScopeCounter.getCombination();
		return Arrays.asList(combination);
	}
}
