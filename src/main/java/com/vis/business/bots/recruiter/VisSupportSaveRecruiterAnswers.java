package com.vis.business.bots.recruiter;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * The "Sim" or "Não" of the operator to the confirmations of the {@code saveRecruiter} command (reactivate an inactive
 * recruiter, inactivate an active one). The answer is compared without spaces around, accents and capitals.
 */
final class VisSupportSaveRecruiterAnswers {

	/** Utility class; not instantiable. */
	private VisSupportSaveRecruiterAnswers() {}

	/** The answers of the operator, without accents and in lower case. */
	enum Answers implements CcpJsonFieldName {
		/** Confirms, in Portuguese. */
		sim,
		/** Confirms, in English. */
		yes,
		/** Does not confirm, in Portuguese. */
		nao,
		/** Does not confirm, in English. */
		no,
	}

	/**
	 * Whether the operator confirmed.
	 * @param session the session, with the answer in {@code typedValue}
	 * @return {@code true} for "Sim" or "yes"
	 */
	static boolean confirmed(CcpJsonRepresentation session) {
		String answer = normalize(session);
		String sim = Answers.sim.getValue();
		String yes = Answers.yes.getValue();
		boolean confirmed = sim.equals(answer) || yes.equals(answer);
		return confirmed;
	}

	/**
	 * Whether the operator refused.
	 * @param session the session, with the answer in {@code typedValue}
	 * @return {@code true} for "Não", "nao" or "no"
	 */
	static boolean refused(CcpJsonRepresentation session) {
		String answer = normalize(session);
		String nao = Answers.nao.getValue();
		String no = Answers.no.getValue();
		boolean refused = nao.equals(answer) || no.equals(answer);
		return refused;
	}

	/**
	 * The answer without spaces around, accents and capitals.
	 * @param session the session, with the answer in {@code typedValue}
	 * @return the answer to compare
	 */
	private static String normalize(CcpJsonRepresentation session) {
		String typedValue = session.getAsString(JnJsonCommonsFields.typedValue);
		String trimmed = typedValue.trim();
		CcpStringDecorator answerDecorator = new CcpStringDecorator(trimmed);
		CcpTextDecorator answerText = answerDecorator.text();
		CcpTextDecorator withoutAccents = answerText.stripAccents();
		String answer = withoutAccents.content.toLowerCase();
		return answer;
	}
}
