package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualResumeTypeCounter;

/**
 * Produces the group of {@code VisEntityVirtualResumeTypeCounter} every resume is counted in: always the same, the only
 * group of the areas of the resumes.
 */
public class VisResumeTypeCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeTypeCounterCombinations INSTANCE = new VisResumeTypeCounterCombinations();

	private VisResumeTypeCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualResumeTypeCounter.getCombination();
		return Arrays.asList(combination);
	}
}
