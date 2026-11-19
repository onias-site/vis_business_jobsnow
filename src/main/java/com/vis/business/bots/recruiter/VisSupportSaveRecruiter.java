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
 * First step of {@code /saveRecruiter <email>} of the support bot (since 2026-11-14): saves the e-mail as a recruiter in
 * {@code vis_recruiter}, with the source {@code support}. The e-mail is normalized as in the other loads (lower case, no
 * spaces around, no accents, {@link VisBusinessPrepareRecruiter}) and must be a valid address: the rules of
 * {@code CcpEmailDecorator.isValid} and the validation of the entity. Both sides of the recruiters are read at once:
 * <ul>
 * <li>an active recruiter: nothing is written yet and the operator is asked whether they want to inactivate them
 * ({@code askInactivation}, since 2026-11-19); the answer goes to {@link VisSupportSaveRecruiterConfirmInactivation};</li>
 * <li>an inactive recruiter ({@code vis_inactive_recruiter}): the operator is asked whether they really want to reactivate
 * them ({@code askReactivation}, since 2026-11-19); the answer goes to {@link VisSupportSaveRecruiterConfirmReactivation};</li>
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
	 * Saves the recruiter, or asks to confirm the reactivation.
	 * @param json the session, with {@code email}
	 * @return the session with the reply {@code saved}
	 * @throws CcpErrorFlowDisturb with {@code invalidEmail}, {@code askInactivation} or {@code askReactivation}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation recruiter = prepare(json);
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		CcpBusiness askInactivation = new VisSupportSaveRecruiterAskInactivation(json, email);
		CcpBusiness askReactivation = new VisSupportSaveRecruiterAskReactivation(json, email);
		CcpJsonRepresentation sessionWithReply = save(this, json, recruiter, askInactivation, askReactivation);
		return sessionWithReply;
	}

	/**
	 * Reads both sides of the recruiters at once and acts on the side where the e-mail is: active or inactive, the given
	 * action; neither, a new recruiter.
	 * @param context the step that runs the procedure
	 * @param session the session of the command
	 * @param recruiter the recruiter, as prepared by {@link #prepare(CcpJsonRepresentation)}
	 * @param whenActive what is done to an active recruiter
	 * @param whenInactive what is done to an inactive recruiter
	 * @return the session with the reply {@code saved}
	 * @throws CcpErrorFlowDisturb with the status of {@code whenActive} or of {@code whenInactive}
	 */
	static CcpJsonRepresentation save(CcpJsonFieldName context, CcpJsonRepresentation session, CcpJsonRepresentation recruiter, CcpBusiness whenActive, CcpBusiness whenInactive) {
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		CcpBusiness insert = new VisSupportSaveRecruiterInsert(recruiter);

		new CcpGetEntityId(recruiter)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityRecruiter.ENTITY).executeAction(whenActive).and()
			.ifThisIdIsPresentInEntity(twinEntity).executeAction(whenInactive).and()
			.executeAction(insert)
			.andFinallyReturningTheseFields(Fields.inexistentField)
		.endThisProcedure(context, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(session, VisSupportSaveRecruiterMessages.saved, email);
		return sessionWithReply;
	}

	/**
	 * The record of the recruiter typed in the command, normalized, transformed and validated by the rules of the entity.
	 * @param session the session, with {@code email}
	 * @return the recruiter, with the source {@code support}
	 * @throws CcpErrorFlowDisturb with {@code invalidEmail} when the address is not valid
	 */
	static CcpJsonRepresentation prepare(CcpJsonRepresentation session) {
		String typedEmail = session.getAsString(JnJsonCommonsFields.email);
		String trimmedEmail = typedEmail.trim();
		String lowerEmail = trimmedEmail.toLowerCase();
		CcpStringDecorator emailText = new CcpStringDecorator(lowerEmail);
		CcpEmailDecorator emailDecorator = emailText.email();
		CcpEmailDecorator emailWithoutAccents = emailDecorator.stripAccents();
		boolean invalidAddress = false == emailWithoutAccents.isValid();

		if(invalidAddress) {
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(session, VisSupportSaveRecruiterMessages.invalidEmail, trimmedEmail);
			return VisSupportSaveRecruiterStatus.invalidEmail.throwException(sessionWithReply);
		}

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
