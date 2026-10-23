package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Action of {@code VisBusinessSkillFixHierarchyReview}, run for each item still pending: an approved item goes to
 * {@code vis_skill_fix_hierarchy_item_approved}, a rejected one to the twin (by deleting it).
 */
class VisBusinessMoveFixHierarchyItem implements CcpBusiness {

	/** The only instance: the action keeps no state. */
	public final static VisBusinessMoveFixHierarchyItem INSTANCE = new VisBusinessMoveFixHierarchyItem();

	private VisBusinessMoveFixHierarchyItem() {
	}

	/**
	 * Moves the item according to its decision.
	 * @param itemWithTheStoredRecord the key of the item with its decision, holding the stored record under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation itemWithTheStoredRecord) {
		CcpJsonRepresentation itemKey = itemWithTheStoredRecord.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type, VisEntitySkillFixHierarchyItemPending.Fields.skill);
		VisSkillFixHierarchyDecisions itemDecision = itemWithTheStoredRecord.getAsEnum(VisSkillFixHierarchyReviewFields.decision, VisSkillFixHierarchyDecisions.class);

		boolean isApproved = VisSkillFixHierarchyDecisions.approved == itemDecision;

		if(isApproved) {
			VisEntitySkillFixHierarchyItemPending.ENTITY.transferDataTo(itemKey, VisEntitySkillFixHierarchyItemApproved.ENTITY);
			return itemWithTheStoredRecord;
		}

		// deleting from a twin entity moves the record to the twin, which holds the rejected items
		VisEntitySkillFixHierarchyItemPending.ENTITY.delete(itemKey);
		return itemWithTheStoredRecord;
	}
}
