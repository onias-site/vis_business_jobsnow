package com.vis.business.skill;

/**
 * After the transfer of a suggestion of skill to {@code vis_skill_approved}: one pending less, one more approved.
 */
public class VisBusinessCountSkillSuggestionApproved extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountSkillSuggestionApproved INSTANCE = new VisBusinessCountSkillSuggestionApproved();

	private VisBusinessCountSkillSuggestionApproved() {
		super(VisSuggestionStatusCounters.skillSuggestion, VisSuggestionStatuses.pending.moveTo(VisSuggestionStatuses.approved));
	}
}
