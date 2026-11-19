package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.vis.entities.VisEntityRecruiter;

/**
 * Step of the {@code saveRecruiter} command after an inactive recruiter was found (since 2026-11-19): does the operator
 * really want to reactivate them? "Sim" (or "yes") brings them back with the source {@code support}
 * ({@link VisSupportSaveRecruiterReactivate}, {@code reactivated}); "Não" (or "nao", "no") writes nothing
 * ({@code reactivationCancelled}). Any other answer is asked again ({@code invalidAnswer}). Both sides are read again on
 * the "Sim": a recruiter that became active or was deleted meanwhile is reported or saved.
 */
public class VisSupportSaveRecruiterConfirmReactivation implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSaveRecruiterConfirmReactivation INSTANCE = new VisSupportSaveRecruiterConfirmReactivation();

	private VisSupportSaveRecruiterConfirmReactivation() {}

	/**
	 * Handles the answer of the operator.
	 * @param json the session, with {@code email} and the answer
	 * @return the session with the reply {@code saved}, when the recruiter was deleted meanwhile
	 * @throws CcpErrorFlowDisturb with {@code reactivated}, {@code reactivationCancelled}, {@code invalidAnswer} or
	 * {@code alreadyRecruiter}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation recruiter = VisSupportSaveRecruiter.prepare(json);
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		boolean reactivate = VisSupportSaveRecruiterAnswers.confirmed(json);
		if(reactivate) {
			CcpBusiness alreadySaved = new VisSupportSaveRecruiterAlreadySaved(json, email);
			CcpBusiness reactivateTheRecruiter = new VisSupportSaveRecruiterReactivate(json, email);
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiter.save(this, json, recruiter, alreadySaved, reactivateTheRecruiter);
			return sessionWithReply;
		}
		boolean doNotReactivate = VisSupportSaveRecruiterAnswers.refused(json);
		if(doNotReactivate) {
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(json, VisSupportSaveRecruiterMessages.reactivationCancelled, email);
			return VisSupportSaveRecruiterStatus.reactivationCancelled.throwException(sessionWithReply);
		}
		CcpJsonRepresentation sessionAskingAgain = VisSupportSaveRecruiterConversation.replyWithTheOptions(json, VisSupportSaveRecruiterMessages.notUnderstood, email);
		return VisSupportSaveRecruiterStatus.invalidAnswer.throwException(sessionAskingAgain);
	}
}
