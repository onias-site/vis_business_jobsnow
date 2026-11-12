package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Produces the key a resume is counted under in the group of each skill it excludes
 * ({@code VisEntityVirtualExcludedSkillCounter}): always {@code resumes}, so the child of the group of a skill is
 * {@code {"resumes": <how many active resumes exclude it>}}.
 */
public class VisResumeExcludedSkillCounterKeys implements Function<CcpJsonRepresentation, List<String>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeExcludedSkillCounterKeys INSTANCE = new VisResumeExcludedSkillCounterKeys();

	private VisResumeExcludedSkillCounterKeys() {}

	/** The keys of the counters. */
	public enum Keys implements CcpJsonFieldName {
		/** How many active resumes exclude the skill of the group. */
		resumes,
	}

	/**
	 * The only key, whatever the resume: the groups it is counted in are its excluded skills.
	 * @param json the resume
	 * @return the key {@code resumes}
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		String key = Keys.resumes.getValue();
		return List.of(key);
	}
}
