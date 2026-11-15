package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.vis.business.recruiter.VisBusinessReactivateRecruiterFromSignUp;
import com.vis.entities.VisEntityRecruiter;

/**
 * Step of the flow of {@code /saveRecruiter}: the e-mail is an inactive recruiter, who goes back to the recruiters with
 * the record they had, as in the sign-up ({@link VisBusinessReactivateRecruiterFromSignUp}, which also keeps the moment
 * of the return so the next e-mail of resumes does not inactivate them again).
 */
class VisSupportSaveRecruiterReactivate implements CcpBusiness {

	/** The session of the command. */
	private final CcpJsonRepresentation session;

	/** The e-mail, normalized. */
	private final String email;

	/**
	 * Keeps what the step needs.
	 * @param session the session of the command
	 * @param email the e-mail, normalized
	 */
	VisSupportSaveRecruiterReactivate(CcpJsonRepresentation session, String email) {
		this.session = session;
		this.email = email;
	}

	/**
	 * Reactivates the recruiter and ends the session telling the operator.
	 * @param json the json of the procedure, holding the inactive record under {@code _entities}
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code reactivated}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		CcpBusiness reactivate = new VisBusinessReactivateRecruiterFromSignUp(twinEntity, this.email);
		reactivate.execute(json);
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(this.session, VisSupportSaveRecruiterMessages.reactivated, this.email);
		return VisSupportSaveRecruiterStatus.reactivated.throwException(sessionWithReply);
	}
}
