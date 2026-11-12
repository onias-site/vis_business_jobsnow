package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualInsertionMonthCounter;

/**
 * Produces the group of {@code VisEntityVirtualInsertionMonthCounter} every record is counted in: always the same, the
 * group of the counters by month of insertion of its entity.
 */
public class VisInsertionMonthCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisInsertionMonthCounterCombinations INSTANCE = new VisInsertionMonthCounterCombinations();

	private VisInsertionMonthCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualInsertionMonthCounter.getCombination();
		return Arrays.asList(combination);
	}
}
