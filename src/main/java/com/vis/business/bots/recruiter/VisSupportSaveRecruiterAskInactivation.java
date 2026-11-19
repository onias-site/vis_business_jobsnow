package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;

/**
 * Step of the flow of the first step of {@code /saveRecruiter} (since 2026-11-19): the e-mail is an active recruiter, so
 * nothing is written and the operator is asked whether they want to inactivate them; the answer goes to
 * {@link VisSupportSaveRecruiterConfirmInactivation}.
 */
class VisSupportSaveRecruiterAskInactivation implements CcpBusiness {

	/** The session of the command. */
	private final CcpJsonRepresentation session;

	/** The e-mail, normalized. */
	private final String email;

	/**
	 * Keeps what the step needs.
	 * @param session the session of the command
	 * @param email the e-mail, normalized
	 */
	VisSupportSaveRecruiterAskInactivation(CcpJsonRepresentation session, String email) {
		this.session = session;
		this.email = email;
	}

	/**
	 * Asks the operator to confirm the inactivation.
	 * @param json the json of the procedure
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code askInactivation}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation sessionWithQuestion = VisSupportSaveRecruiterConversation.replyWithTheOptions(this.session, VisSupportSaveRecruiterMessages.confirmInactivation, this.email);
		return VisSupportSaveRecruiterStatus.askInactivation.throwException(sessionWithQuestion);
	}
}
