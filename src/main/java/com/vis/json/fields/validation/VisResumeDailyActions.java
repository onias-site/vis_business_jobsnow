package com.vis.json.fields.validation;

/**
 * The actions over a saved resume that the candidate may run only once a day each ({@code VisEntityResumeDailyAction}):
 * every one of them emails the candidate and loads the backend, so without this limit the screen of the resume could
 * drain the email quota. Inserting a resume (saving when there is none) is not one of them: it has no limit.
 */
public enum VisResumeDailyActions {
	/** Deactivating the active resume (it moves to {@code vis_inactive_resume}). */
	inactivation,
	/** Reactivating the inactive resume (it moves back to {@code vis_resume}). */
	reactivation,
	/** Deleting the resume for good, wherever it is. */
	exclusion,
	/** Saving a resume that already exists, active or inactive. */
	update,
	;
}
