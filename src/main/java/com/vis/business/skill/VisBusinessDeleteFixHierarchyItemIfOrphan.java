package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryBool;
import com.ccp.especifications.db.query.CcpQueryMust;
import com.ccp.especifications.db.query.CcpQueryMustNot;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Action of {@code VisBusinessSkillFixHierarchyDeleteOrphanItems}, run for each item of the deleted request still
 * pending: discards the item when no other pending request lists it.
 */
class VisBusinessDeleteFixHierarchyItemIfOrphan implements CcpBusiness {

	/** The deleted request. */
	private final CcpJsonRepresentation deletedRequest;

	/**
	 * Keeps the deleted request.
	 * @param deletedRequest the deleted request
	 */
	VisBusinessDeleteFixHierarchyItemIfOrphan(CcpJsonRepresentation deletedRequest) {
		this.deletedRequest = deletedRequest;
	}

	/**
	 * Discards the item when it became an orphan.
	 * @param itemWithTheStoredRecord the key of the item, holding the stored record under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation itemWithTheStoredRecord) {
		String skill = itemWithTheStoredRecord.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		boolean anotherRequestAsksForIt = this.anotherRequestAsksFor(skill);

		if(anotherRequestAsksForIt) {
			return itemWithTheStoredRecord;
		}

		CcpJsonRepresentation itemKey = itemWithTheStoredRecord.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type, VisEntitySkillFixHierarchyItemPending.Fields.skill);
		// a plain delete would move the item to the twin, which holds the rejected items
		VisEntitySkillFixHierarchyItemPending.ENTITY.deleteAnyWhere(itemKey);
		return itemWithTheStoredRecord;
	}

	/**
	 * Whether a pending request of another user, with the same parent and type, lists the skill. The deleted
	 * request is excluded by its email (the json arrives transformed, with the same hash the index keeps),
	 * because the search may still see it for a moment after the deletion.
	 */
	private boolean anotherRequestAsksFor(String skill) {

		String parent = this.deletedRequest.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		String type = this.deletedRequest.getAsString(VisEntitySkillFixHierarchyPending.Fields.type);
		String email = this.deletedRequest.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);

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
