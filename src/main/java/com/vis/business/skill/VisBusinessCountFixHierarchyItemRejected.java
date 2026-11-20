package com.vis.business.skill;

/**
 * After the delete of a pending item of hierarchy, which moves it to its twin {@code vis_skill_fix_hierarchy_item_rejected}: one pending less, one more rejected, in the group of its {@code type}.
 */
public class VisBusinessCountFixHierarchyItemRejected extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountFixHierarchyItemRejected INSTANCE = new VisBusinessCountFixHierarchyItemRejected();

	private VisBusinessCountFixHierarchyItemRejected() {
		super(VisSuggestionStatusCounters.skillFixHierarchyItem, VisSuggestionStatuses.pending.moveTo(VisSuggestionStatuses.rejected));
	}
}
