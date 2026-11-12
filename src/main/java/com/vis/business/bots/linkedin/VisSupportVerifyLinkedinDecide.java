package com.vis.business.bots.linkedin;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Second step of the {@code verifyLinkedinAddress} command: is the recruiter right in saying that the LinkedIn address
 * is invalid? "Sim" (or "yes") marks the resume ({@link VisSupportVerifyLinkedinConfirmInvalid}, {@code linkedinInvalid});
 * "Não" (or "nao", "no") tells the recruiter that the profile exists ({@link VisSupportVerifyLinkedinConfirmValid},
 * {@code linkedinValid}). Both close the ticket. Any other answer is asked again ({@code invalidAnswer}). The answer is
 * compared without accents and case.
 */
public class VisSupportVerifyLinkedinDecide implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportVerifyLinkedinDecide INSTANCE = new VisSupportVerifyLinkedinDecide();

	private VisSupportVerifyLinkedinDecide() {}

	/** The answers of the operator, without accents and in lower case. */
	enum Answers implements CcpJsonFieldName {
		/** The recruiter is right, in Portuguese. */
		sim,
		/** The recruiter is right, in English. */
		yes,
		/** The recruiter is wrong, in Portuguese. */
		nao,
		/** The recruiter is wrong, in English. */
		no,
	}

	/**
	 * Handles the answer of the operator.
	 * @param json the session, with the parameters of the command and the answer
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code linkedinInvalid}, {@code linkedinValid} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = normalize(typedValue);
		String sim = Answers.sim.getValue();
		String yes = Answers.yes.getValue();
		boolean recruiterIsRight = sim.equals(answer) || yes.equals(answer);
		if(recruiterIsRight) {
			VisSupportVerifyLinkedinConfirmInvalid.INSTANCE.execute(json);
			CcpJsonRepresentation jsonWithReply = VisSupportVerifyLinkedinConversation.replyAboutTheTicket(json, VisSupportVerifyLinkedinMessages.confirmedInvalid);
			return VisSupportVerifyLinkedinStatus.linkedinInvalid.throwException(jsonWithReply);
		}
		String nao = Answers.nao.getValue();
		String no = Answers.no.getValue();
		boolean recruiterIsWrong = nao.equals(answer) || no.equals(answer);
		if(recruiterIsWrong) {
			VisSupportVerifyLinkedinConfirmValid.INSTANCE.execute(json);
			CcpJsonRepresentation jsonWithReply = VisSupportVerifyLinkedinConversation.replyAboutTheTicket(json, VisSupportVerifyLinkedinMessages.confirmedValid);
			return VisSupportVerifyLinkedinStatus.linkedinValid.throwException(jsonWithReply);
		}
		CcpJsonRepresentation jsonAskingAgain = VisSupportVerifyLinkedinConversation.replyWithTheOptions(json, VisSupportVerifyLinkedinMessages.notUnderstood);
		return VisSupportVerifyLinkedinStatus.invalidAnswer.throwException(jsonAskingAgain);
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
