package com.vis.business.skill;

/**
 * After the delete of a rejected suggestion of skill: one rejected less.
 */
public class VisBusinessCountRejectedSkillSuggestionDeleted extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountRejectedSkillSuggestionDeleted INSTANCE = new VisBusinessCountRejectedSkillSuggestionDeleted();

	private VisBusinessCountRejectedSkillSuggestionDeleted() {
		super(VisSuggestionStatusCounters.skillSuggestion, VisSuggestionStatuses.rejected.leave());
	}
}
