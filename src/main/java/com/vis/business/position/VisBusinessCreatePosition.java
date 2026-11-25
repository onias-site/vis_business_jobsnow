package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidatorEngine;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.entities.JnEntityEmailSuppressed;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;
import com.vis.status.VisProcessStatusSavePosition;
import com.vis.json.fields.validation.VisJsonFieldsNewExpirationRange;

/**
 * "Criar vaga" of the screen "Meu Recrutamento - Vagas": saves a new position of the logged-in user, whose e-mail
 * becomes the {@code recruiter}. The title and the month of the creation ({@code yearMonth}, the current one) are part of
 * the primary key: a title the user already has among the positions not deleted created in the same month answers 409. The expiration of a new position starts and ends
 * from the current date on. The save is asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 409} when the user already has a position with this title
 */
public class VisBusinessCreatePosition implements CcpBusiness {

	/** The fields of the procedure of the save. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The single instance. */
	public static final VisBusinessCreatePosition INSTANCE = new VisBusinessCreatePosition();

	private VisBusinessCreatePosition() {}

	public Class<?> getJsonValidationClass() {
		return VisPositionOfTheRecruiterFields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation input) {
		// the month of the creation is part of the key (since 2026-11-24): always the current one, whatever came in the input
		String currentYearMonth = VisPositionOfTheRecruiter.getCurrentYearMonth();
		CcpJsonRepresentation json = input.put(VisPositionOfTheRecruiterFields.yearMonth, currentYearMonth);
		CcpJsonRepresentation expiration = json.getInnerJson(VisPositionOfTheRecruiterFields.expiration);
		String expirationName = VisPositionOfTheRecruiterFields.expiration.name();
		CcpJsonValidatorEngine.INSTANCE.validateJson(VisJsonFieldsNewExpirationRange.class, expiration, expirationName);
		CcpJsonRepresentation position = VisPositionOfTheRecruiter.fromInput(json);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		CcpBusiness save = new VisBusinessSavePosition(position);
		// the e-mail that receives the resumes, as the key of the suppressions; absent (frequency none), the step is skipped
		CcpJsonRepresentation resumesEmail = json.getJsonPiece(VisPositionOfTheRecruiterFields.resumesEmail);
		CcpJsonRepresentation suppressionKey = resumesEmail.renameField(VisPositionOfTheRecruiterFields.resumesEmail, JnEntityEmailSuppressed.Fields.email);

		new CcpGetEntityId(key, suppressionKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.CONFLICT).and()
			.ifThisIdIsPresentInEntity(JnEntityEmailSuppressed.ENTITY).returnStatus(VisProcessStatusSavePosition.suppressedEmail).and()
			.executeAction(save)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = position.removeFields(VisEntityPosition.Fields.recruiter);
		return response;
	}
}
