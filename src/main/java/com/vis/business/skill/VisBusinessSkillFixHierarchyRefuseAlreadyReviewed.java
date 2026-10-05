package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Runs before every save of a {@link VisEntitySkillFixHierarchyPending} request: refuses it when every skill of
 * its {@code skill} array was already reviewed for the same parent and type in earlier requests, that is, when
 * each item (parent + type + skill) is already in {@link VisEntitySkillFixHierarchyItemApproved} or in
 * vis_skill_fix_hierarchy_item_rejected (the twin of {@link VisEntitySkillFixHierarchyItemPending}). In that case
 * there is nothing left for the operator to decide, so {@link VisErrorSkillFixHierarchyAlreadyReviewed} is thrown,
 * the entity's global handler tells the user by email and the request is not saved.
 *
 * <p>A request with at least one skill not yet reviewed goes on, and {@link VisBusinessSkillFixHierarchyCreateItems}
 * creates items only for the skills not yet reviewed.
 */
public class VisBusinessSkillFixHierarchyRefuseAlreadyReviewed implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation itemKeyWithoutSkill = json.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type);
		List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);
		CcpEntity rejectedItemEntity = VisEntitySkillFixHierarchyItemPending.ENTITY.getTwinEntity();

		for (String skill : skills) {
			CcpJsonRepresentation itemKey = itemKeyWithoutSkill.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill);

			boolean itemWasApproved = VisEntitySkillFixHierarchyItemApproved.ENTITY.exists(itemKey);

			if(itemWasApproved) {
				continue;
			}

			boolean itemWasRejected = rejectedItemEntity.exists(itemKey);

			if(itemWasRejected) {
				continue;
			}

			return json;
		}

		VisErrorSkillFixHierarchyAlreadyReviewed alreadyReviewed = new VisErrorSkillFixHierarchyAlreadyReviewed(json);
		throw alreadyReviewed;
	}
}
