package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;

/**
 * Before the delete everywhere of an item of hierarchy (the item no pending request names any more,
 * {@code VisBusinessDeleteFixHierarchyItemIfOrphan}, or the cleaning of a test): the item leaves the status of the side
 * it is on, pending on the main side or rejected on the twin side, in the counter of its {@code type}
 * ({@link VisSuggestionStatusCounters}); an item on neither side changes nothing.
 */
public class VisBusinessCountFixHierarchyItemDeletedEverywhere implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessCountFixHierarchyItemDeletedEverywhere INSTANCE = new VisBusinessCountFixHierarchyItemDeletedEverywhere();

	private VisBusinessCountFixHierarchyItemDeletedEverywhere() {}

	/** The field returned by the reading procedure, which answers nothing. */
	enum JsonFieldNames implements CcpJsonFieldName {
		/** The procedure returns no field. */
		inexistentField,
	}

	/**
	 * Takes the item out of the counter of the status of its side.
	 * @param json the key of the item, with its {@code type}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation pendingLeft = VisSuggestionStatuses.pending.leave();
		CcpJsonRepresentation rejectedLeft = VisSuggestionStatuses.rejected.leave();
		CcpBusiness leavePending = new VisBusinessCountSuggestionStatus(VisSuggestionStatusCounters.skillFixHierarchyItem, pendingLeft);
		CcpBusiness leaveRejected = new VisBusinessCountSuggestionStatus(VisSuggestionStatusCounters.skillFixHierarchyItem, rejectedLeft);
		CcpEntity rejectedItems = VisEntitySkillFixHierarchyItemPending.ENTITY.getTwinEntity();

		new CcpGetEntityId(json)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntitySkillFixHierarchyItemPending.ENTITY).executeAction(leavePending).and()
			.ifThisIdIsPresentInEntity(rejectedItems).executeAction(leaveRejected)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}
}
