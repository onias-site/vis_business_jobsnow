package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;

/** Step of the flow of {@code /saveRecruiter}: the e-mail is already an active recruiter, nothing is written. */
class VisSupportSaveRecruiterAlreadySaved implements CcpBusiness {

	/** The session of the command. */
	private final CcpJsonRepresentation session;

	/** The e-mail, normalized. */
	private final String email;

	/**
	 * Keeps what the step needs.
	 * @param session the session of the command
	 * @param email the e-mail, normalized
	 */
	VisSupportSaveRecruiterAlreadySaved(CcpJsonRepresentation session, String email) {
		this.session = session;
		this.email = email;
	}

	/**
	 * Ends the session telling the operator.
	 * @param json the json of the procedure
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code alreadyRecruiter}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(this.session, VisSupportSaveRecruiterMessages.alreadyRecruiter, this.email);
		return VisSupportSaveRecruiterStatus.alreadyRecruiter.throwException(sessionWithReply);
	}
}
