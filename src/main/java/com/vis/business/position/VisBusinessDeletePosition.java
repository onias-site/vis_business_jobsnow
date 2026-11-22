package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;

/**
 * "Excluir" of the screen "Meu Recrutamento - Vagas": moves a position of the logged-in user to the twin
 * {@code vis_inactive_position}, so it leaves both tabs of the screen (they read only the main groups). The write is
 * asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the user has no position with this title
 */
public class VisBusinessDeletePosition implements CcpBusiness {

	/** The fields of the procedure. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The single instance. */
	public static final VisBusinessDeletePosition INSTANCE = new VisBusinessDeletePosition();

	private VisBusinessDeletePosition() {}

	public Class<?> getJsonValidationClass() {
		return VisPositionKeyFields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(VisPositionKeyFields.email);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		CcpBusiness delete = new VisBusinessDeleteStoredPosition(email);

		new CcpGetEntityId(key)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(VisEntityPosition.ENTITY).executeAction(delete)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = json.getJsonPiece(VisPositionKeyFields.title);
		return response;
	}
}
