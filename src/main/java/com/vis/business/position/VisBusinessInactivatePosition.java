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
 * "Inativar" of the screen "Meu Recrutamento - Vagas": the position of the logged-in user expires one minute before now,
 * so it goes to the tab of the expired positions; it stays in {@code vis_position} (only "Excluir" moves it to the twin).
 * The write is asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the user has no position with this title
 */
public class VisBusinessInactivatePosition implements CcpBusiness {

	/** How long before now the inactivated position expires (milliseconds). */
	private static final long ONE_MINUTE = 60_000L;

	/** The single instance. */
	public static final VisBusinessInactivatePosition INSTANCE = new VisBusinessInactivatePosition();

	private VisBusinessInactivatePosition() {}

	public Class<?> getJsonValidationClass() {
		return VisPositionKeyFields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(VisPositionKeyFields.email);
		long now = System.currentTimeMillis();
		Long max = now - ONE_MINUTE;
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		CcpBusiness expire = new VisBusinessChangePositionExpiration(email, max);

		CcpJsonRepresentation result = new CcpGetEntityId(key)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(VisEntityPosition.ENTITY).executeAction(expire)
			.andFinallyReturningTheseFields(VisEntityPosition.Fields.expiration)
		.endThisProcedureRetrievingTheResultingData(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation title = json.getJsonPiece(VisPositionKeyFields.title);
		CcpJsonRepresentation response = title.mergeWithAnotherJson(result);
		return response;
	}
}
