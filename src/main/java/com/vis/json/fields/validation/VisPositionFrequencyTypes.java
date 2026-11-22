package com.vis.json.fields.validation;

/**
 * Frequencies accepted by the {@code frequencyType} of the position: how often the matching resumes are sent to the
 * recruiter.
 */
public enum VisPositionFrequencyTypes {
	/** Never. */
	none,
	/** As soon as a matching resume shows up. */
	instantly,
	/** Every hour. */
	hourly,
	/** Every day. */
	daily,
	/** Every week. */
	weekly
	;
}
