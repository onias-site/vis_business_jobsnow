package com.vis.business.skill;

/**
 * After the delete of an approved item of hierarchy: one approved less, in the group of its {@code type}.
 */
public class VisBusinessCountApprovedFixHierarchyItemDeleted extends VisBusinessCountSuggestionStatus {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountApprovedFixHierarchyItemDeleted INSTANCE = new VisBusinessCountApprovedFixHierarchyItemDeleted();

	private VisBusinessCountApprovedFixHierarchyItemDeleted() {
		super(VisSuggestionStatusCounters.skillFixHierarchyItem, VisSuggestionStatuses.approved.leave());
	}
}
