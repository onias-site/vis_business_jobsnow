package com.vis.business.recruiter.resumes;

import com.vis.json.fields.validation.VisPositionFrequencyTypes;

/**
 * The topic of the scheduled job that sends the positions of the frequency {@code hourly} the resumes that match them
 * among the ones added or changed in the last hour; runs every hour, with the purge of the disposable records (job {@code jobsnow-reenvio-token-login}) (since 2026-11-24).
 */
public class VisBusinessSendHourlyResumesOfThePositions extends VisBusinessSendResumesOfThePositionsOfThePeriod {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendHourlyResumesOfThePositions INSTANCE = new VisBusinessSendHourlyResumesOfThePositions();

	private VisBusinessSendHourlyResumesOfThePositions() {
		super(VisPositionFrequencyTypes.hourly, 3_600_000L);
	}
}