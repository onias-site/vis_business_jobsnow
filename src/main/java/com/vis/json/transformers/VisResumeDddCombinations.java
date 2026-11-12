package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualDddGrouper;

/**
 * Produces the groups of one region per ddd of the record, the ddd as an integer, so the input and the stored record
 * give the same ids: a resume in the groups of {@code VisEntityVirtualDddGrouper}, a view of a resume (whose
 * {@code ddd} holds the ddds of the resume at the view) in the groups of {@code VisEntityVirtualViewedResumeDddGrouper}.
 * Both parents have the same primary key, {@code ddd}.
 */
public class VisResumeDddCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeDddCombinations INSTANCE = new VisResumeDddCombinations();

	private VisResumeDddCombinations() {}

	/**
	 * One combination per ddd of the record.
	 * @param json the resume or the view
	 * @return the combinations, {@code {"ddd": <ddd>}}
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		List<Object> ddds = json.getAsObjectList(VisEntityResume.Fields.ddd);
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (Object ddd : ddds) {
			CcpJsonRepresentation rawCombination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualDddGrouper.Fields.ddd, ddd);
			Integer dddAsInteger = rawCombination.getAsIntegerNumber(VisEntityVirtualDddGrouper.Fields.ddd);
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualDddGrouper.Fields.ddd, dddAsInteger);
			combinations.add(combination);
		}
		return combinations;
	}
}
