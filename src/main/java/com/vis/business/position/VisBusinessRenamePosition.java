package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;

/**
 * Step of the edition that changes the title of a position (since 2026-11-24), run once the position of the old title
 * was found: the old and the new keys are of the same entity, so the new one is checked in a read of its own. A new
 * title the recruiter already has in the month of the creation answers 409; a free one replaces the position
 * ({@link VisBusinessReplaceTheRenamedPosition}).
 *
 * @throws CcpErrorFlowDisturb {@code 409} when the new title is of another position of the same recruiter and month
 */
public class VisBusinessRenamePosition implements CcpBusiness {

	/** The position with the new title. */
	private final CcpJsonRepresentation newPosition;

	/** The key of the position with the new title. */
	private final CcpJsonRepresentation newKey;

	/** The key of the position with the old title. */
	private final CcpJsonRepresentation oldKey;

	/**
	 * Keeps the positions.
	 * @param newPosition the position with the new title
	 * @param newKey the key of the position with the new title
	 * @param oldKey the key of the position with the old title
	 */
	public VisBusinessRenamePosition(CcpJsonRepresentation newPosition, CcpJsonRepresentation newKey, CcpJsonRepresentation oldKey) {
		this.newPosition = newPosition;
		this.newKey = newKey;
		this.oldKey = oldKey;
	}

	/**
	 * Checks the new title and replaces the position.
	 * @param json the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpBusiness replace = new VisBusinessReplaceTheRenamedPosition(this.newPosition, this.oldKey);
		new CcpGetEntityId(this.newKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.CONFLICT).and()
			.executeAction(replace)
			.andFinallyReturningTheseFields(VisBusinessEditPosition.ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}
}
