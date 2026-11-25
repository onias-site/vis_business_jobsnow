package com.vis.business.position;

import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.mensageria.JnBusinessSendToMensageria;
import com.vis.entities.VisEntityPositionResumeSent;

/**
 * The renaming of a position (since 2026-11-24) carries the resumes already sent to it ({@link VisEntityPositionResumeSent})
 * from the id of the old title to the id of the new one, so the candidates who already received the e-mail of the position
 * do not receive it again: each pair is saved under the new id and deleted from the old one. Runs in the consumer of the
 * messaging, never in the online request of the edition, since it searches the pairs by position (OQL).
 */
public class VisBusinessMoveTheResumesSentToTheRenamedPosition implements JnBusinessSendToMensageria {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessMoveTheResumesSentToTheRenamedPosition INSTANCE = new VisBusinessMoveTheResumesSentToTheRenamedPosition();

	private VisBusinessMoveTheResumesSentToTheRenamedPosition() {}

	/** The fields of the input and of the result. */
	public static enum Fields implements CcpJsonFieldName {
		/** The id of the position of the old title. */
		oldPosition,
		/** The id of the position of the new title. */
		newPosition,
		/** How many pairs were carried. */
		moved,
	}

	/**
	 * Carries the pairs of the old position to the new one.
	 * @param json {@code oldPosition} and {@code newPosition}
	 * @return the input plus {@code moved}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String oldPosition = json.getAsString(Fields.oldPosition);
		String newPosition = json.getAsString(Fields.newPosition);
		// the search sees only what was refreshed, and a pair may have been written moments ago
		CcpEntityMetaData entityDetails = VisEntityPositionResumeSent.ENTITY.getEntityMetaData();
		CcpDbRequester dbRequester = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		dbRequester.refresh(entityDetails.entityName);
		CcpQueryOptions pairsOfTheOldPosition = CcpQueryOptions.INSTANCE
				.startQuery()
				.startBool()
				.startMust()
				.term(VisEntityPositionResumeSent.Fields.position, oldPosition)
				.endMustAndBackToBool()
				.endBoolAndBackToQuery()
				.endQueryAndBackToRequest()
				.maxResults();
		CcpQueryExecutorDecorator pairsQuery = pairsOfTheOldPosition.selectFrom(VisEntityPositionResumeSent.ENTITY);
		List<CcpJsonRepresentation> pairs = pairsQuery.getResultAsList();
		for (CcpJsonRepresentation pair : pairs) {
			CcpJsonRepresentation oldPair = pair.getJsonPiece(VisEntityPositionResumeSent.Fields.values());
			CcpJsonRepresentation newPair = oldPair.put(VisEntityPositionResumeSent.Fields.position, newPosition);
			VisEntityPositionResumeSent.ENTITY.save(newPair);
			VisEntityPositionResumeSent.ENTITY.delete(oldPair);
		}
		int moved = pairs.size();
		CcpJsonRepresentation result = json.put(Fields.moved, moved);
		return result;
	}
}
