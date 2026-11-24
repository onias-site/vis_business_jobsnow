package com.vis.business.recruiter.resumes;

import com.vis.json.fields.validation.VisPositionFrequencyTypes;

/**
 * The topic of the scheduled job that sends the positions of the frequency {@code daily} the resumes that match them
 * among the ones added or changed in the last 24 hours; runs every day, right after the backup (job {@code jobsnow-backup-diario}, 00:10) (since 2026-11-24).
 */
public class VisBusinessSendDailyResumesOfThePositions extends VisBusinessSendResumesOfThePositionsOfThePeriod {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendDailyResumesOfThePositions INSTANCE = new VisBusinessSendDailyResumesOfThePositions();

	private VisBusinessSendDailyResumesOfThePositions() {
		super(VisPositionFrequencyTypes.daily, 86_400_000L);
	}
}