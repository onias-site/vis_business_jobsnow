package com.vis.json.transformers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualTicketKindCounter;

/**
 * Produces the group of {@code VisEntityVirtualTicketKindCounter} every ticket of vis is counted in: always the same
 * combination (the entity of the ticket tells the groups apart).
 */
public class VisTicketKindCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisTicketKindCounterCombinations INSTANCE = new VisTicketKindCounterCombinations();

	private VisTicketKindCounterCombinations() {}

	/**
	 * The combination of the group, whatever the record.
	 * @param json the record
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation combination = VisEntityVirtualTicketKindCounter.getCombination();
		return Arrays.asList(combination);
	}
}
