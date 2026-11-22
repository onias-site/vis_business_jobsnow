package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeAfter;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;

/**
 * "Reativar" of the screen "Meu Recrutamento - Vagas": an expired position of the logged-in user gets the new end of the
 * expiration the user filled in (required, from the current date on), so it goes back to the tab of the positions not
 * expired. The write is asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the user has no position with this title
 */
public class VisBusinessReactivatePosition implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The e-mail of the logged-in user, the recruiter of the position. */
		@CcpJsonCopyFieldValidationsFrom(VisPositionKeyFields.class)
		email,

		/** The title of the position. */
		@CcpJsonCopyFieldValidationsFrom(VisPositionKeyFields.class)
		title,

		/** The new end of the expiration: required, timestamp (milliseconds) from the current date on. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeTimeAfter(minValue = 0, intervalType = CcpEntityExpurgableOptions.daily)
		max,
	}

	/** The single instance. */
	public static final VisBusinessReactivatePosition INSTANCE = new VisBusinessReactivatePosition();

	private VisBusinessReactivatePosition() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(Fields.email);
		Long max = json.getAsLongNumber(Fields.max);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		CcpBusiness renew = new VisBusinessChangePositionExpiration(email, max);

		CcpJsonRepresentation result = new CcpGetEntityId(key)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(VisEntityPosition.ENTITY).executeAction(renew)
			.andFinallyReturningTheseFields(VisEntityPosition.Fields.expiration)
		.endThisProcedureRetrievingTheResultingData(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation title = json.getJsonPiece(Fields.title);
		CcpJsonRepresentation response = title.mergeWithAnotherJson(result);
		return response;
	}
}
