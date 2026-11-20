package com.vis.business.skill;

/**
 * After the insertion of a suggestion of skill: one more pending.
 */
public class VisBusinessCountSkillSuggestionInserted extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountSkillSuggestionInserted INSTANCE = new VisBusinessCountSkillSuggestionInserted();

	private VisBusinessCountSkillSuggestionInserted() {
		super(VisSuggestionStatusCounters.skillSuggestion, VisSuggestionStatuses.pending.enter());
	}
}
