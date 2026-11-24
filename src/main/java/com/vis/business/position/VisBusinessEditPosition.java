package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.entities.JnEntityEmailSuppressed;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;
import com.vis.status.VisProcessStatusSavePosition;

/**
 * "Editar" of the screen "Meu Recrutamento - Vagas": saves again a position of the logged-in user with what they
 * changed. The title is part of the primary key and is not changed by the edition (the front end does not let it be
 * edited). The save is asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the user has no position with this title
 */
public class VisBusinessEditPosition implements CcpBusiness {

	/** The fields of the procedure of the save. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The single instance. */
	public static final VisBusinessEditPosition INSTANCE = new VisBusinessEditPosition();

	private VisBusinessEditPosition() {}

	public Class<?> getJsonValidationClass() {
		return VisPositionOfTheRecruiterFields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation position = VisPositionOfTheRecruiter.fromInput(json);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		CcpBusiness save = new VisBusinessSavePosition(position);
		// the e-mail that receives the resumes, as the key of the suppressions; absent (frequency none), the step is skipped
		CcpJsonRepresentation resumesEmail = json.getJsonPiece(VisPositionOfTheRecruiterFields.resumesEmail);
		CcpJsonRepresentation suppressionKey = resumesEmail.renameField(VisPositionOfTheRecruiterFields.resumesEmail, JnEntityEmailSuppressed.Fields.email);

		new CcpGetEntityId(key, suppressionKey)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(JnEntityEmailSuppressed.ENTITY).returnStatus(VisProcessStatusSavePosition.suppressedEmail).and()
			.executeAction(save)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = position.removeFields(VisEntityPosition.Fields.recruiter);
		return response;
	}
}
