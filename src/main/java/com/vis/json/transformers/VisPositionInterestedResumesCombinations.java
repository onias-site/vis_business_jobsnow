package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityVirtualPositionInterestedResumes;

/**
 * Produces the group of {@link VisEntityVirtualPositionInterestedResumes} an interest of a candidate belongs to: the
 * position and the hour of its sending, where the resume is a child.
 */
public class VisPositionInterestedResumesCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisPositionInterestedResumesCombinations INSTANCE = new VisPositionInterestedResumesCombinations();

	private VisPositionInterestedResumesCombinations() {}

	/**
	 * The combination of the position and the hour of the sending.
	 * @param json the record of {@link VisEntityPositionInterest}
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		String position = json.getAsString(VisEntityPositionInterest.Fields.position);
		String sendingHour = json.getAsString(VisEntityPositionInterest.Fields.sendingHour);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityVirtualPositionInterestedResumes.Fields.position, position)
				.put(VisEntityVirtualPositionInterestedResumes.Fields.sendingHour, sendingHour);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
