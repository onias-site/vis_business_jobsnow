package com.vis.business.skill;

/**
 * After the transfer of a suggestion of skill to {@code vis_skill_rejected}: one pending less, one more rejected.
 */
public class VisBusinessCountSkillSuggestionRejected extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountSkillSuggestionRejected INSTANCE = new VisBusinessCountSkillSuggestionRejected();

	private VisBusinessCountSkillSuggestionRejected() {
		super(VisSuggestionStatusCounters.skillSuggestion, VisSuggestionStatuses.pending.moveTo(VisSuggestionStatuses.rejected));
	}
}
