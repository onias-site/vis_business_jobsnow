package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;

/**
 * The key of the counter of resumes by area ({@code counterKeyClass} of a {@code @CcpEntityGrouperChildCounter} counted
 * by {@code JnGrouperChildCurrentCounterEntityBeforeWrite}): the {@code resumeType} of the stored resume, as an integer
 * (a number read from the database may come as 1.0); no key when the resume has none.
 */
public class VisResumeTypeCounterKeys implements Function<CcpJsonRepresentation, List<String>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeTypeCounterKeys INSTANCE = new VisResumeTypeCounterKeys();

	private VisResumeTypeCounterKeys() {}

	/**
	 * The area of the resume.
	 * @param json the resume as stored
	 * @return its area, empty when it has none
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		boolean noResumeType = false == json.containsAllFields(VisEntityResume.Fields.resumeType);

		if(noResumeType) {
			return List.of();
		}

		Integer resumeType = json.getAsIntegerNumber(VisEntityResume.Fields.resumeType);
		String key = String.valueOf(resumeType);
		return List.of(key);
	}
}
