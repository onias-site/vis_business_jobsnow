package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityRecruiter;

/**
 * Second step of the {@code saveRecruiter} command (since 2026-11-19): does the operator really want to reactivate the
 * inactive recruiter? "Sim" (or "yes") brings them back with the source {@code support}
 * ({@link VisSupportSaveRecruiterReactivate}, {@code reactivated}); "Não" (or "nao", "no") writes nothing
 * ({@code reactivationCancelled}). Any other answer is asked again ({@code invalidAnswer}). The answer is compared without
 * accents and case. Both sides are read again on the "Sim": a recruiter that became active or was deleted meanwhile is
 * reported or saved as in the first step.
 */
public class VisSupportSaveRecruiterConfirmReactivation implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSaveRecruiterConfirmReactivation INSTANCE = new VisSupportSaveRecruiterConfirmReactivation();

	private VisSupportSaveRecruiterConfirmReactivation() {}

	/** The answers of the operator, without accents and in lower case. */
	enum Answers implements CcpJsonFieldName {
		/** Reactivate, in Portuguese. */
		sim,
		/** Reactivate, in English. */
		yes,
		/** Do not reactivate, in Portuguese. */
		nao,
		/** Do not reactivate, in English. */
		no,
	}

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
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = normalize(typedValue);
		String sim = Answers.sim.getValue();
		String yes = Answers.yes.getValue();
		boolean reactivate = sim.equals(answer) || yes.equals(answer);
		if(reactivate) {
			CcpBusiness reactivateTheRecruiter = new VisSupportSaveRecruiterReactivate(json, email);
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiter.save(this, json, recruiter, reactivateTheRecruiter);
			return sessionWithReply;
		}
		String nao = Answers.nao.getValue();
		String no = Answers.no.getValue();
		boolean doNotReactivate = nao.equals(answer) || no.equals(answer);
		if(doNotReactivate) {
			CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(json, VisSupportSaveRecruiterMessages.reactivationCancelled, email);
			return VisSupportSaveRecruiterStatus.reactivationCancelled.throwException(sessionWithReply);
		}
		CcpJsonRepresentation sessionAskingAgain = VisSupportSaveRecruiterConversation.replyWithTheOptions(json, VisSupportSaveRecruiterMessages.notUnderstood, email);
		return VisSupportSaveRecruiterStatus.invalidAnswer.throwException(sessionAskingAgain);
	}

	/**
	 * The answer without spaces around, accents and capitals.
	 * @param typedValue the answer as typed
	 * @return the answer to compare
	 */
	private static String normalize(String typedValue) {
		String trimmed = typedValue.trim();
		CcpStringDecorator answerDecorator = new CcpStringDecorator(trimmed);
		CcpTextDecorator answerText = answerDecorator.text();
		CcpTextDecorator withoutAccents = answerText.stripAccents();
		String answer = withoutAccents.content.toLowerCase();
		return answer;
	}
}
