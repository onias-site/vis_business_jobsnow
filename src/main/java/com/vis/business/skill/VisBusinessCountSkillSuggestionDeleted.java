package com.vis.business.skill;

/**
 * After the delete of a pending suggestion of skill (the candidate gave it up, or the user became ignored): one pending less.
 */
public class VisBusinessCountSkillSuggestionDeleted extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountSkillSuggestionDeleted INSTANCE = new VisBusinessCountSkillSuggestionDeleted();

	private VisBusinessCountSkillSuggestionDeleted() {
		super(VisSuggestionStatusCounters.skillSuggestion, VisSuggestionStatuses.pending.leave());
	}
}
