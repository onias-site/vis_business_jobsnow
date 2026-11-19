package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityRecruiter;

/**
 * Step of the {@code saveRecruiter} command after an active recruiter was found (since 2026-11-19): does the operator want
 * to inactivate them? "Sim" (or "yes") records the unsubscription in {@code vis_recruiter_unsubscribed} and moves the
 * recruiter to {@code vis_inactive_recruiter}, as the link of unsubscription of the e-mail of resumes
 * ({@link VisSupportSaveRecruiterInactivate}, {@code inactivated}); "Não" (or "nao", "no") writes nothing
 * ({@code inactivationCancelled}). Any other answer is asked again ({@code invalidAnswer}). The recruiter is read again on
 * the "Sim": one no longer active meanwhile ends with {@code noLongerActive}, nothing written.
 */
public class VisSupportSaveRecruiterConfirmInactivation implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSaveRecruiterConfirmInactivation INSTANCE = new VisSupportSaveRecruiterConfirmInactivation();

	private VisSupportSaveRecruiterConfirmInactivation() {}

	/** Fields of the procedure. */
	enum Fields implements CcpJsonFieldName {
		/** The procedure returns no field. */
		inexistentField,
	}

	/**
	 * Handles the answer of the operator.
	 * @param json the session, with {@code email} and the answer
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code inactivated}, {@code inactivationCancelled}, {@code invalidAnswer} or
	 * {@code noLongerActive}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation recruiter = VisSupportSaveRecruiter.prepare(json);
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		boolean inactivate = VisSupportSaveRecruiterAnswers.confirmed(json);
		if(inactivate) {
			return this.inactivate(json, recruiter, email);
		}
		boolean doNotInactivate = VisSupportSaveRecruiterAnswers.refused(json);
		if(doNotInactivate) {
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(json, VisSupportSaveRecruiterMessages.inactivationCancelled, email);
			return VisSupportSaveRecruiterStatus.inactivationCancelled.throwException(sessionWithReply);
		}
		CcpJsonRepresentation sessionAskingAgain = VisSupportSaveRecruiterConversation.replyWithTheOptions(json, VisSupportSaveRecruiterMessages.notUnderstood, email);
		return VisSupportSaveRecruiterStatus.invalidAnswer.throwException(sessionAskingAgain);
	}

	/**
	 * Inactivates the recruiter still active; one no longer active is only reported.
	 * @param session the session of the command
	 * @param recruiter the recruiter, as prepared
	 * @param email the e-mail, normalized
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code inactivated} or {@code noLongerActive}
	 */
	private CcpJsonRepresentation inactivate(CcpJsonRepresentation session, CcpJsonRepresentation recruiter, String email) {
		CcpBusiness inactivateTheRecruiter = new VisSupportSaveRecruiterInactivate(session, email);
		CcpBusiness noLongerActive = new VisSupportSaveRecruiterNoLongerActive(session, email);
		new CcpGetEntityId(recruiter)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityRecruiter.ENTITY).executeAction(inactivateTheRecruiter).and()
			.executeAction(noLongerActive)
			.andFinallyReturningTheseFields(Fields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return session;
	}
}
