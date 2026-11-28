package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityPositionResumeSent;
import com.vis.entities.VisEntityResume;
import com.vis.status.VisProcessStatusPositionInterest;

/**
 * "Enviar meu currículo" of the screen "Meus dados → Minhas vagas" (since 2026-11-24): the candidate logged in confirms
 * the interest in a position open to the candidates, and their resume goes to the recruiter in the next full hour, with
 * the resumes of the other interested candidates ({@link VisEntityPositionInterest}). Both sides are checked at the
 * confirmation, in one read of the presence checks:
 * <ul>
 * <li>the resume inactive: 301; missing: 404;</li>
 * <li>the interest already registered, or the resume already sent to the position: 409;</li>
 * <li>then, with the stored resume and position, {@link VisBusinessSaveThePositionInterest}: the position missing (404)
 * or expired (410), the resume blocked by a rule of its own, a restriction to correct or the LinkedIn confirmed invalid
 * (422), the position not open to the resume (404).</li>
 * </ul>
 * Answers the hour of the sending ({@code sendingHour}, {@code yyyyMMddHH}).
 */
public class VisBusinessRegisterInterestInThePosition implements CcpBusiness {

	/** The input of the business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The candidate logged in. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		/** The id of the position, as the screen received it. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		position,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessRegisterInterestInThePosition INSTANCE = new VisBusinessRegisterInterestInThePosition();

	private VisBusinessRegisterInterestInThePosition() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(Fields.email);
		String positionId = json.getAsString(Fields.position);
		String emailHash = VisBusinessSendResumesToRecruiters.getEmailHash(email);
		CcpJsonRepresentation key = VisBusinessPositionsOfTheResume.getSendingKey(positionId, email, emailHash);
		CcpEntity inactiveResume = VisEntityResume.ENTITY.getTwinEntity();
		CcpBusiness saveTheInterest = new VisBusinessSaveThePositionInterest(positionId, email);

		CcpJsonRepresentation resultingData = new CcpGetEntityId(key)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(inactiveResume).returnStatus(CcpProcessStatusDefault.REDIRECT).and()
			.ifThisIdIsNotPresentInEntity(VisEntityResume.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(VisEntityPositionInterest.ENTITY).returnStatus(VisProcessStatusPositionInterest.alreadyInterested).and()
			.ifThisIdIsPresentInEntity(VisEntityPositionResumeSent.ENTITY).returnStatus(VisProcessStatusPositionInterest.alreadyInterested).and()
			.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY).executeAction(saveTheInterest)
			.andFinallyReturningTheseFields(VisEntityPositionInterest.Fields.sendingHour)
		.endThisProcedureRetrievingTheResultingData(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = resultingData.getJsonPiece(VisEntityPositionInterest.Fields.sendingHour);
		return response;
	}
}
