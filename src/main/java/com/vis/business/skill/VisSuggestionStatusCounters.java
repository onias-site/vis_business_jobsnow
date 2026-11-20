package com.vis.business.skill;

import java.util.Map;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntityVirtualSuggestionStatusCounter;

/**
 * The countings of the suggestions of the candidates by status ({@link VisSuggestionStatuses}), each one a group of
 * {@link VisEntityVirtualSuggestionStatusCounter} on the main side of {@code jn_grouper}, whose only child is the JSON
 * text of the counters. The transfers of the review go to the database in bulk, out of the decorators of the entities,
 * so the counters are corrected by the side effects of each write and transfer ({@code VisBusinessCount*}) and by the
 * bulk that creates the items of hierarchy ({@link VisBusinessSkillFixHierarchyCreateItems}), never by reading the
 * entities.
 */
public enum VisSuggestionStatusCounters {
	/** The suggestions of a new skill, a single group (child {@code vis_skill_pending}). */
	skillSuggestion {
		public CcpJsonRepresentation getCombination(CcpJsonRepresentation suggestion) {
			String counterName = this.name();
			CcpJsonRepresentation combination = VisEntityVirtualSuggestionStatusCounter.getCombination(counterName);
			return combination;
		}

		public CcpEntity getChildEntity() {
			return VisEntitySkillPending.ENTITY;
		}
	},
	/**
	 * The items of the suggestions of hierarchy, one group per {@code type} of the item ({@code add}: association,
	 * {@code remove}: dissociation), child {@code vis_skill_fix_hierarchy_item_pending}.
	 */
	skillFixHierarchyItem {
		public CcpJsonRepresentation getCombination(CcpJsonRepresentation item) {
			String type = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
			CcpJsonRepresentation combination = VisEntityVirtualSuggestionStatusCounter.getCombination(type);
			return combination;
		}

		public CcpEntity getChildEntity() {
			return VisEntitySkillFixHierarchyItemPending.ENTITY;
		}
	},
	;

	/**
	 * The combination of the group the suggestion is counted in.
	 * @param suggestion the suggestion (an item of hierarchy must have its {@code type})
	 * @return the combination of {@link VisEntityVirtualSuggestionStatusCounter}
	 */
	public abstract CcpJsonRepresentation getCombination(CcpJsonRepresentation suggestion);

	/**
	 * The entity counted, whose name is the {@code childName} of the group.
	 * @return the entity
	 */
	public abstract CcpEntity getChildEntity();

	/**
	 * Adds the change of a suggestion to the counters of its group (atomically, by the database); a counter that reaches
	 * 0 leaves the group.
	 * @param suggestion the suggestion
	 * @param deltas the amount to add to each status, as {@link VisSuggestionStatuses} produces it
	 */
	public void addToCounters(CcpJsonRepresentation suggestion, CcpJsonRepresentation deltas) {
		CcpJsonRepresentation combination = this.getCombination(suggestion);
		String parentId = VisEntityVirtualSuggestionStatusCounter.ENTITY.calculateId(combination);
		CcpEntityMetaData parentDetails = VisEntityVirtualSuggestionStatusCounter.ENTITY.getEntityMetaData();
		CcpEntity childEntity = this.getChildEntity();
		CcpEntityMetaData childDetails = childEntity.getEntityMetaData();
		Map<String, CcpJsonRepresentation> deltasByParentId = Map.of(parentId, deltas);
		Map<String, CcpJsonRepresentation> parents = Map.of(parentId, combination);
		CcpEntity grouper = JnGroupers.getGrouper(false);
		JnGroupers.addToCounters(grouper, parentDetails.entityName, childDetails.entityName, deltasByParentId, parents);
	}
}
