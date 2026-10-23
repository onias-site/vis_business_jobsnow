package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Runs after a {@link VisEntitySkillFixHierarchyPending} request is deleted (the user withdrew it, for instance):
 * discards the {@link VisEntitySkillFixHierarchyItemPending} items of its skills that no other pending request
 * still asks for.
 *
 * <p>An item has no email in its key (parent + type + skill), so it is shared by every user who asks for the
 * same change: an item that another pending request lists stays, otherwise that request would lose it. Items
 * already decided (approved, or rejected in the twin) are not touched, they are the history of the reviews.
 * Up to 2026-09-30 a withdrawn request left its items pending forever.
 */
public class VisBusinessSkillFixHierarchyDeleteOrphanItems implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessSkillFixHierarchyDeleteOrphanItems INSTANCE = new VisBusinessSkillFixHierarchyDeleteOrphanItems();

	private VisBusinessSkillFixHierarchyDeleteOrphanItems() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation itemKeyWithoutSkill = json.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type);
		List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);
		boolean noSkill = skills.isEmpty();

		if(noSkill) {
			return json;
		}

		int skillsCount = skills.size();
		CcpJsonRepresentation[] itemKeys = new CcpJsonRepresentation[skillsCount];

		for (int index = 0; index < skillsCount; index++) {
			String skill = skills.get(index);
			itemKeys[index] = itemKeyWithoutSkill.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill);
		}

		CcpBusiness deleteIfOrphan = new VisBusinessDeleteFixHierarchyItemIfOrphan(json);

		// every item is read by id in a single union all; the action runs only for the items still pending
		new CcpGetEntityId(itemKeys)
		.toBeginProcedureAnd()
			.forEachIdPresentInEntity(VisEntitySkillFixHierarchyItemPending.ENTITY).executeAction(deleteIfOrphan)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;

		return json;
	}

	/** The procedure answers nothing: the business returns the json it received. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field. */
		inexistentField
	}
}
