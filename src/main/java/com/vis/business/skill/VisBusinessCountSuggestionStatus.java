package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * The base of the side effects that correct the counters of the suggestions by status ({@link VisSuggestionStatusCounters})
 * after a write or a transfer of a suggestion: the subclasses, named by the annotations of the entities, fix the counting
 * and the change of status.
 */
public class VisBusinessCountSuggestionStatus implements CcpBusiness {

	/** The counting the suggestion belongs to. */
	private final VisSuggestionStatusCounters counters;

	/** The amount added to each status. */
	private final CcpJsonRepresentation deltas;

	/**
	 * Keeps the counting and the change.
	 * @param counters the counting the suggestion belongs to
	 * @param deltas the amount added to each status
	 */
	public VisBusinessCountSuggestionStatus(VisSuggestionStatusCounters counters, CcpJsonRepresentation deltas) {
		this.counters = counters;
		this.deltas = deltas;
	}

	/**
	 * Adds the change to the counters of the group of the suggestion.
	 * @param json the suggestion written or transferred
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		this.counters.addToCounters(json, this.deltas);
		return json;
	}
}
