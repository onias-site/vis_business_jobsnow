package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.utils.JnDeleteKeysFromCache;
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
 * <p>Every item is searched in both entities with a single union all, instead of one lookup per item and entity.
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

		boolean notYetReviewed = false == allItemsAlreadyReviewed(json);

		if(notYetReviewed) {
			return json;
		}

		VisErrorSkillFixHierarchyAlreadyReviewed alreadyReviewed = new VisErrorSkillFixHierarchyAlreadyReviewed(json);
		throw alreadyReviewed;
	}

	/**
	 * Tells whether every skill of the request was already reviewed (approved or rejected) for the same parent and type;
	 * a request with no skill counts as reviewed. Also used by the service, which tells the user at once.
	 * @param json the request
	 * @return {@code true} when nothing is left for the operator to decide
	 */
	public static boolean allItemsAlreadyReviewed(CcpJsonRepresentation json) {

		CcpJsonRepresentation itemKeyWithoutSkill = json.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type);
		List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);
		boolean noSkill = skills.isEmpty();

		if(noSkill) {
			return true;
		}

		CcpJsonRepresentation[] itemKeys = skills.stream()
				.map(skill -> itemKeyWithoutSkill.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill))
				.toArray(CcpJsonRepresentation[]::new);

		CcpEntity rejectedItemEntity = VisEntitySkillFixHierarchyItemPending.ENTITY.getTwinEntity();
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll reviewedItems = crud.unionAll(itemKeys, JnDeleteKeysFromCache.INSTANCE, VisEntitySkillFixHierarchyItemApproved.ENTITY, rejectedItemEntity);

		for (CcpJsonRepresentation itemKey : itemKeys) {

			boolean itemWasApproved = VisEntitySkillFixHierarchyItemApproved.ENTITY.isPresentInThisUnionAll(reviewedItems, itemKey);

			if(itemWasApproved) {
				continue;
			}

			boolean itemWasRejected = rejectedItemEntity.isPresentInThisUnionAll(reviewedItems, itemKey);

			if(itemWasRejected) {
				continue;
			}

			return false;
		}

		return true;
	}
}
