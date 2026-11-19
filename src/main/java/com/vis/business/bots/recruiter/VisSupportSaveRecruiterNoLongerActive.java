package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;

/**
 * Step of the flow of the confirmation of the inactivation of {@code /saveRecruiter} (since 2026-11-19): the recruiter
 * stopped being active between the question and the "Sim", so nothing is written.
 */
class VisSupportSaveRecruiterNoLongerActive implements CcpBusiness {

	/** The session of the command. */
	private final CcpJsonRepresentation session;

	/** The e-mail, normalized. */
	private final String email;

	/**
	 * Keeps what the step needs.
	 * @param session the session of the command
	 * @param email the e-mail, normalized
	 */
	VisSupportSaveRecruiterNoLongerActive(CcpJsonRepresentation session, String email) {
		this.session = session;
		this.email = email;
	}

	/**
	 * Ends the session telling the operator.
	 * @param json the json of the procedure
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code noLongerActive}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(this.session, VisSupportSaveRecruiterMessages.noLongerActive, this.email);
		return VisSupportSaveRecruiterStatus.noLongerActive.throwException(sessionWithReply);
	}
}
