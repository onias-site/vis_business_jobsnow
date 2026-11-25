package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;

/**
 * "Restaurar" of the tab of the deleted positions of the screen "Meu Recrutamento - Vagas" (since 2026-11-24): a position
 * of the logged-in user found in the twin {@code vis_inactive_position} goes back to {@code vis_position}, valid from the
 * beginning of the current day until the end of the day one week later. When the user has a position not deleted with the
 * same title, the restored one gets the title followed by the first free number ("title 2"). The write is asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the user has no deleted position with this title
 */
public class VisBusinessRestorePosition implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessRestorePosition INSTANCE = new VisBusinessRestorePosition();

	private VisBusinessRestorePosition() {}

	public Class<?> getJsonValidationClass() {
		return VisPositionKeyFields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(VisPositionKeyFields.email);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		CcpEntity twinEntity = VisEntityPosition.ENTITY.getTwinEntity();
		CcpBusiness restore = new VisBusinessRestoreStoredPosition(email);

		CcpJsonRepresentation result = new CcpGetEntityId(key)
		.toBeginProcedureAnd()
			.loadThisIdFromEntity(VisEntityPosition.ENTITY).and()
			.ifThisIdIsNotPresentInEntity(twinEntity).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(twinEntity).executeAction(restore)
			.andFinallyReturningTheseFields(VisEntityPosition.Fields.title, VisEntityPosition.Fields.expiration)
		.endThisProcedureRetrievingTheResultingData(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return result;
	}
}
