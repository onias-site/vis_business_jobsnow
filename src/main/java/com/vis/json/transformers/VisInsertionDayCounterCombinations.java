package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualInsertionDayCounter;

/**
 * Produces the group of {@code VisEntityVirtualInsertionDayCounter} every record is counted in: always the same, the
 * group of the counters by day of insertion of its entity.
 */
public class VisInsertionDayCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisInsertionDayCounterCombinations INSTANCE = new VisInsertionDayCounterCombinations();

	private VisInsertionDayCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualInsertionDayCounter.getCombination();
		return Arrays.asList(combination);
	}
}
