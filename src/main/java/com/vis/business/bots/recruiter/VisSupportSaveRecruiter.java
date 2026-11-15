package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.recruiter.VisBusinessPrepareRecruiter;
import com.vis.entities.VisEntityRecruiter;

/**
 * Single step of {@code /saveRecruiter <email>} of the support bot (since 2026-11-14): saves the e-mail as a recruiter in
 * {@code vis_recruiter}, with the source {@code support}. The e-mail is normalized as in the other loads (lower case, no
 * spaces around, no accents, {@link VisBusinessPrepareRecruiter}) and must be a valid address: the rules of
 * {@code CcpEmailDecorator.isValid} and the validation of the entity. Both sides of the recruiters are read at once:
 * <ul>
 * <li>already an active recruiter: nothing is written ({@code alreadyRecruiter});</li>
 * <li>an inactive recruiter ({@code vis_inactive_recruiter}): goes back to the recruiters with the record they had, as in
 * the sign-up ({@code reactivated});</li>
 * <li>neither: a new recruiter, and the step ends with the reply {@code saved}.</li>
 * </ul>
 */
public class VisSupportSaveRecruiter implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSaveRecruiter INSTANCE = new VisSupportSaveRecruiter();

	private VisSupportSaveRecruiter() {}

	/** Fields of the procedure. */
	enum Fields implements CcpJsonFieldName {
		/** The procedure returns no field. */
		inexistentField,
	}

	/**
	 * Saves the recruiter.
	 * @param json the session, with {@code email}
	 * @return the session with the reply {@code saved}
	 * @throws CcpErrorFlowDisturb with {@code invalidEmail}, {@code alreadyRecruiter} or {@code reactivated}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String typedEmail = json.getAsString(JnJsonCommonsFields.email);
		String trimmedEmail = typedEmail.trim();
		String lowerEmail = trimmedEmail.toLowerCase();
		CcpStringDecorator emailText = new CcpStringDecorator(lowerEmail);
		CcpEmailDecorator emailDecorator = emailText.email();
		CcpEmailDecorator emailWithoutAccents = emailDecorator.stripAccents();
		boolean invalidAddress = false == emailWithoutAccents.isValid();

		if(invalidAddress) {
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(json, VisSupportSaveRecruiterMessages.invalidEmail, trimmedEmail);
			return VisSupportSaveRecruiterStatus.invalidEmail.throwException(sessionWithReply);
		}

		CcpJsonRepresentation recruiter = prepare(json, trimmedEmail);
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		CcpBusiness alreadySaved = new VisSupportSaveRecruiterAlreadySaved(json, email);
		CcpBusiness reactivate = new VisSupportSaveRecruiterReactivate(json, email);
		CcpBusiness insert = new VisSupportSaveRecruiterInsert(recruiter);

		new CcpGetEntityId(recruiter)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityRecruiter.ENTITY).executeAction(alreadySaved).and()
			.ifThisIdIsPresentInEntity(twinEntity).executeAction(reactivate).and()
			.executeAction(insert)
			.andFinallyReturningTheseFields(Fields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(json, VisSupportSaveRecruiterMessages.saved, email);
		return sessionWithReply;
	}

	/**
	 * The record of the recruiter, normalized, transformed and validated by the rules of the entity.
	 * @param session the session, for the reply
	 * @param trimmedEmail the e-mail as typed, without the spaces around
	 * @return the recruiter, with the source {@code support}
	 * @throws CcpErrorFlowDisturb with {@code invalidEmail} when the entity refuses the address
	 */
	private static CcpJsonRepresentation prepare(CcpJsonRepresentation session, String trimmedEmail) {
		String supportSource = VisEntityRecruiter.Sources.support.getValue();
		CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, trimmedEmail);
		CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, supportSource);
		try {
			CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);
			return recruiter;
		} catch (CcpJsonValidationError refusedByTheEntity) {
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(session, VisSupportSaveRecruiterMessages.invalidEmail, trimmedEmail);
			return VisSupportSaveRecruiterStatus.invalidEmail.throwException(sessionWithReply);
		}
	}
}
