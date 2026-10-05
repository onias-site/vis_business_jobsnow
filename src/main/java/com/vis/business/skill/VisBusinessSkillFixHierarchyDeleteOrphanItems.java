package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryBool;
import com.ccp.especifications.db.query.CcpQueryMust;
import com.ccp.especifications.db.query.CcpQueryMustNot;
import com.ccp.especifications.db.query.CcpQueryOptions;
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

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation itemKeyWithoutSkill = json.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type);
		List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);

		for (String skill : skills) {
			CcpJsonRepresentation itemKey = itemKeyWithoutSkill.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill);
			boolean itemIsNotPending = false == VisEntitySkillFixHierarchyItemPending.ENTITY.exists(itemKey);

			if(itemIsNotPending) {
				continue;
			}

			boolean anotherRequestAsksForIt = this.anotherRequestAsksFor(json, skill);

			if(anotherRequestAsksForIt) {
				continue;
			}

			// a plain delete would move the item to the twin, which holds the rejected items
			VisEntitySkillFixHierarchyItemPending.ENTITY.deleteAnyWhere(itemKey);
		}

		return json;
	}

	/**
	 * Whether a pending request of another user, with the same parent and type, lists the skill. The deleted
	 * request is excluded by its email (the json arrives transformed, with the same hash the index keeps),
	 * because the search may still see it for a moment after the deletion.
	 */
	private boolean anotherRequestAsksFor(CcpJsonRepresentation deletedRequest, String skill) {

		String parent = deletedRequest.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		String type = deletedRequest.getAsString(VisEntitySkillFixHierarchyPending.Fields.type);
		String email = deletedRequest.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);

		CcpQueryBool bool = CcpQueryOptions.INSTANCE
				.startQuery()
				.startBool();
		CcpQueryMust mustWithTheParent = bool
				.startMust()
				.term(VisEntitySkillFixHierarchyPending.Fields.parent, parent);
		CcpQueryMust mustWithTheType = mustWithTheParent
				.term(VisEntitySkillFixHierarchyPending.Fields.type, type);
		CcpQueryMust mustWithTheSkill = mustWithTheType
				.term(VisEntitySkillFixHierarchyPending.Fields.skill, skill);
		CcpQueryBool boolWithTheItem = mustWithTheSkill
				.endMustAndBackToBool();
		CcpQueryMustNot mustNotBeTheDeletedRequest = boolWithTheItem
				.startMustNot()
				.term(VisEntitySkillFixHierarchyPending.Fields.email, email);
		CcpQueryOptions otherRequestsWithTheItem = mustNotBeTheDeletedRequest
				.endMustNotAndBackToBool()
				.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();

		long total = otherRequestsWithTheItem
				.selectFrom(VisEntitySkillFixHierarchyPending.ENTITY)
				.total();

		boolean anotherRequestAsksForIt = total > 0;
		return anotherRequestAsksForIt;
	}
}
