package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityRecruiter;

/**
 * The key of the counter of the recruiters by source ({@code counterKeyClass} of a {@code @CcpEntityGrouperChildCounter}
 * counted by {@code JnGrouperChildCurrentCounterEntityBeforeWrite}): the {@code source} of the stored recruiter; no key
 * when it has none.
 */
public class VisRecruiterSourceCounterKeys implements Function<CcpJsonRepresentation, List<String>>{
	/** The single instance: the class keeps no state. */
	public final static VisRecruiterSourceCounterKeys INSTANCE = new VisRecruiterSourceCounterKeys();

	private VisRecruiterSourceCounterKeys() {}

	/**
	 * The source of the recruiter.
	 * @param json the recruiter as stored
	 * @return its source, empty when it has none
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		boolean noSource = false == json.containsAllFields(VisEntityRecruiter.Fields.source);

		if(noSource) {
			return List.of();
		}

		String source = json.getAsString(VisEntityRecruiter.Fields.source);
		return List.of(source);
	}
}
