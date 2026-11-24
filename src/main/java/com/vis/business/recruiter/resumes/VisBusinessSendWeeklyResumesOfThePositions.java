package com.vis.business.recruiter.resumes;

import com.vis.json.fields.validation.VisPositionFrequencyTypes;

/**
 * The topic of the scheduled job that sends the positions of the frequency {@code weekly} the resumes that match them
 * among the ones added or changed in the last 7 days; runs on Mondays at 9:30 (job {@code jobsnow-curriculos-semanais-das-vagas}) (since 2026-11-24).
 */
public class VisBusinessSendWeeklyResumesOfThePositions extends VisBusinessSendResumesOfThePositionsOfThePeriod {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendWeeklyResumesOfThePositions INSTANCE = new VisBusinessSendWeeklyResumesOfThePositions();

	private VisBusinessSendWeeklyResumesOfThePositions() {
		super(VisPositionFrequencyTypes.weekly, 604_800_000L);
	}
}