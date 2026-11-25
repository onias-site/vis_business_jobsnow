package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityVirtualPositionCandidates;

/**
 * Produces the group of {@link VisEntityVirtualPositionCandidates} an interest of a candidate belongs to: the position,
 * where the resume is a child whatever the hour of its sending.
 */
public class VisPositionCandidatesCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisPositionCandidatesCombinations INSTANCE = new VisPositionCandidatesCombinations();

	private VisPositionCandidatesCombinations() {}

	/**
	 * The combination of the position.
	 * @param json the record of {@link VisEntityPositionInterest}
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		String position = json.getAsString(VisEntityPositionInterest.Fields.position);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualPositionCandidates.Fields.position, position);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
