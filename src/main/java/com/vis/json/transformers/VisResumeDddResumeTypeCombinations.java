package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualDddResumeTypeGrouper;

/**
 * Produces the groups of {@code VisEntityVirtualDddResumeTypeGrouper} of a resume: one per ddd of the resume, each with
 * the area of the resume (resumeType), both as integers, so the input and the stored record give the same ids.
 */
public class VisResumeDddResumeTypeCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeDddResumeTypeCombinations INSTANCE = new VisResumeDddResumeTypeCombinations();

	private VisResumeDddResumeTypeCombinations() {}

	/**
	 * One combination per ddd of the resume, all with the area of the resume.
	 * @param json the resume
	 * @return the combinations, {@code {"ddd": <ddd>, "resumeType": <area>}}
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		boolean withoutResumeType = false == json.containsAllFields(VisEntityResume.Fields.resumeType);

		if(withoutResumeType) {
			return combinations;
		}

		Integer resumeType = json.getAsIntegerNumber(VisEntityResume.Fields.resumeType);
		List<Object> ddds = json.getAsObjectList(VisEntityResume.Fields.ddd);
		for (Object ddd : ddds) {
			CcpJsonRepresentation rawCombination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd, ddd);
			Integer dddAsInteger = rawCombination.getAsIntegerNumber(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd);
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd, dddAsInteger)
					.put(VisEntityVirtualDddResumeTypeGrouper.Fields.resumeType, resumeType);
			combinations.add(combination);
		}
		return combinations;
	}
}
