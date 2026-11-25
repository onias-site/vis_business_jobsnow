package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;

/**
 * The last step of the renaming of a position (since 2026-11-24), when the new title is free: saves the position under
 * the new title, makes the one of the old title disappear without a trace ({@code deleteAnyWhere}: neither in the
 * deleted positions nor in their groups) and sends to the messaging the move of the resumes already sent to it
 * ({@link VisBusinessMoveTheResumesSentToTheRenamedPosition}).
 */
public class VisBusinessReplaceTheRenamedPosition implements CcpBusiness {

	/** The position with the new title. */
	private final CcpJsonRepresentation newPosition;

	/** The key of the position with the old title. */
	private final CcpJsonRepresentation oldKey;

	/**
	 * Keeps both positions.
	 * @param newPosition the position with the new title
	 * @param oldKey the key of the position with the old title
	 */
	public VisBusinessReplaceTheRenamedPosition(CcpJsonRepresentation newPosition, CcpJsonRepresentation oldKey) {
		this.newPosition = newPosition;
		this.oldKey = oldKey;
	}

	/**
	 * Replaces the position.
	 * @param json the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the ids are calculated before the writes, from the recruiter as the entity stores it (its hash)
		CcpJsonRepresentation hashedOldKey = VisEntityPosition.ENTITY.getJsonWithHandledPrimaryKey(this.oldKey);
		CcpJsonRepresentation hashedNewPosition = VisEntityPosition.ENTITY.getJsonWithHandledPrimaryKey(this.newPosition);
		String oldPositionId = VisEntityPosition.ENTITY.calculateId(hashedOldKey);
		String newPositionId = VisEntityPosition.ENTITY.calculateId(hashedNewPosition);
		VisEntityPosition.ENTITY.save(this.newPosition);
		VisEntityPosition.ENTITY.deleteAnyWhere(this.oldKey);
		CcpJsonRepresentation move = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessMoveTheResumesSentToTheRenamedPosition.Fields.oldPosition, oldPositionId)
				.put(VisBusinessMoveTheResumesSentToTheRenamedPosition.Fields.newPosition, newPositionId);
		VisBusinessMoveTheResumesSentToTheRenamedPosition.INSTANCE.sendToMensageria(move);
		return json;
	}
}
