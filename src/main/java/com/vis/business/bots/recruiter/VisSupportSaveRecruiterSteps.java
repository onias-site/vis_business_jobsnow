package com.vis.business.bots.recruiter;

/**
 * Steps of the {@code saveRecruiter} command after the first one (the first step has the name of the command itself).
 */
public enum VisSupportSaveRecruiterSteps {
	/** The answer of the operator: reactivate the inactive recruiter? (since 2026-11-19) */
	saveRecruiterConfirmReactivation,
	/** The answer of the operator: inactivate the active recruiter? (since 2026-11-19) */
	saveRecruiterConfirmInactivation
}
