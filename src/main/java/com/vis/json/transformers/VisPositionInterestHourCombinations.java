package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityVirtualPositionInterestHour;

/**
 * Produces the group of {@link VisEntityVirtualPositionInterestHour} an interest of a candidate belongs to: the hour of
 * its sending, where the position is a child.
 */
public class VisPositionInterestHourCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisPositionInterestHourCombinations INSTANCE = new VisPositionInterestHourCombinations();

	private VisPositionInterestHourCombinations() {}

	/**
	 * The combination of the hour of the sending.
	 * @param json the record of {@link VisEntityPositionInterest}
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		String sendingHour = json.getAsString(VisEntityPositionInterest.Fields.sendingHour);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualPositionInterestHour.Fields.sendingHour, sendingHour);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
