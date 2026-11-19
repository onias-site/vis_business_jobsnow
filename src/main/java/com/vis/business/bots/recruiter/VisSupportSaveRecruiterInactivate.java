package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpBusinessDeleteStoredRecordFromEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterUnsubscribed;

/**
 * Step of the flow of the confirmation of the inactivation of {@code /saveRecruiter} (since 2026-11-19): the operator
 * confirmed, so the recruiter is recorded as unsubscribed ({@code vis_recruiter_unsubscribed}) and moved to the inactive
 * recruiters ({@code vis_inactive_recruiter}), the same writes of the link of unsubscription of the e-mail of resumes.
 */
class VisSupportSaveRecruiterInactivate implements CcpBusiness {

	/** The session of the command. */
	private final CcpJsonRepresentation session;

	/** The e-mail, normalized. */
	private final String email;

	/**
	 * Keeps what the step needs.
	 * @param session the session of the command
	 * @param email the e-mail, normalized
	 */
	VisSupportSaveRecruiterInactivate(CcpJsonRepresentation session, String email) {
		this.session = session;
		this.email = email;
	}

	/**
	 * Records the unsubscription, inactivates the recruiter and ends the session telling the operator.
	 * @param json the json of the procedure, holding the active record under {@code _entities}
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code inactivated}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation unsubscribed = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterUnsubscribed.Fields.email, this.email);
		VisEntityRecruiterUnsubscribed.ENTITY.save(unsubscribed);
		// deleting from the recruiters moves the stored record to the twin
		CcpBusiness inactivate = new CcpBusinessDeleteStoredRecordFromEntity(VisEntityRecruiter.ENTITY);
		inactivate.execute(json);
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(this.session, VisSupportSaveRecruiterMessages.inactivated, this.email);
		return VisSupportSaveRecruiterStatus.inactivated.throwException(sessionWithReply);
	}
}
