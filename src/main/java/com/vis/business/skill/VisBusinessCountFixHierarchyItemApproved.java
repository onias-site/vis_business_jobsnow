package com.vis.business.skill;

/**
 * After the transfer of an item of hierarchy to {@code vis_skill_fix_hierarchy_item_approved}: one pending less, one more approved, in the group of its {@code type}.
 */
public class VisBusinessCountFixHierarchyItemApproved extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountFixHierarchyItemApproved INSTANCE = new VisBusinessCountFixHierarchyItemApproved();

	private VisBusinessCountFixHierarchyItemApproved() {
		super(VisSuggestionStatusCounters.skillFixHierarchyItem, VisSuggestionStatuses.pending.moveTo(VisSuggestionStatuses.approved));
	}
}
