package com.vis.utils;

/**
 * Classifies how a required skill of a position was found in the candidate's resume during
 * the matching process.
 */
enum ResumeSkillFoundType {
	/** The resume lists the skill itself. */
	CONTAINED_IN_RESUME,
	/** The resume lists a synonym of the skill. */
	SYNONYM,
	/** The resume lists skills whose parent is the skill. */
	PARENT
}
