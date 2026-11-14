package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualDddCounter;

/**
 * Produces the group of {@code VisEntityVirtualDddCounter} every record is counted in: always the same, the group of the
 * counters by ddd of its entity.
 */
public class VisDddCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisDddCounterCombinations INSTANCE = new VisDddCounterCombinations();

	private VisDddCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualDddCounter.getCombination();
		return Arrays.asList(combination);
	}
}
