package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Produces the key a view of a resume is counted under in the group of the resume
 * ({@code VisEntityVirtualResumeViewsCounter}): always {@code views}, so the child of the group of a resume is
 * {@code {"views": <how many recruiters ever viewed it>}}.
 */
public class VisResumeViewsCounterKeys implements Function<CcpJsonRepresentation, String>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeViewsCounterKeys INSTANCE = new VisResumeViewsCounterKeys();

	private VisResumeViewsCounterKeys() {}

	/** The keys of the counters. */
	public enum Keys implements CcpJsonFieldName {
		/** How many views the resume of the group ever had, each recruiter once. */
		views,
	}

	/**
	 * The only key, whatever the view: the groups it is counted in are the resumes of its LinkedIn address.
	 * @param json the view
	 * @return the key {@code views}
	 */
	public String apply(CcpJsonRepresentation json) {
		String key = Keys.views.getValue();
		return key;
	}
}
