package com.vis.business.skill;

/**
 * After the delete of an approved suggestion of skill: one approved less.
 */
public class VisBusinessCountApprovedSkillSuggestionDeleted extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountApprovedSkillSuggestionDeleted INSTANCE = new VisBusinessCountApprovedSkillSuggestionDeleted();

	private VisBusinessCountApprovedSkillSuggestionDeleted() {
		super(VisSuggestionStatusCounters.skillSuggestion, VisSuggestionStatuses.approved.leave());
	}
}
