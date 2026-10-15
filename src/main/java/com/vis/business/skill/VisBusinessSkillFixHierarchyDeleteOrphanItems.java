package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.query.CcpQueryBool;
import com.ccp.especifications.db.query.CcpQueryMust;
import com.ccp.especifications.db.query.CcpQueryMustNot;
import com.ccp.especifications.db.query.CcpQueryOptions;
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

		CcpBusiness deleteIfOrphan = itemWithTheStoredRecord -> {
			String skill = itemWithTheStoredRecord.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
			boolean anotherRequestAsksForIt = this.anotherRequestAsksFor(json, skill);

			if(anotherRequestAsksForIt) {
				return itemWithTheStoredRecord;
			}

			CcpJsonRepresentation itemKey = itemWithTheStoredRecord.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type, VisEntitySkillFixHierarchyItemPending.Fields.skill);
			// a plain delete would move the item to the twin, which holds the rejected items
			VisEntitySkillFixHierarchyItemPending.ENTITY.deleteAnyWhere(itemKey);
			return itemWithTheStoredRecord;
		};

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
