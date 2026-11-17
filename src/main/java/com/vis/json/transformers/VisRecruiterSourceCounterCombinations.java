package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualRecruiterSourceCounter;

/**
 * Produces the group of {@code VisEntityVirtualRecruiterSourceCounter} every recruiter is counted in: always the same,
 * the only group of the sources of the recruiters.
 */
public class VisRecruiterSourceCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisRecruiterSourceCounterCombinations INSTANCE = new VisRecruiterSourceCounterCombinations();

	private VisRecruiterSourceCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualRecruiterSourceCounter.getCombination();
		return Arrays.asList(combination);
	}
}
