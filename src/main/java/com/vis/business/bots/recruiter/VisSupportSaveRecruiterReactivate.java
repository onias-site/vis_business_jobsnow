package com.vis.business.bots.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.CcpEntity.JsonFieldNames;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.vis.business.recruiter.VisBusinessReactivateRecruiterFromSignUp;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterUnsubscribed;

/**
 * Step of the flow of the confirmation of {@code /saveRecruiter}: the operator confirmed the reactivation of the inactive
 * recruiter, who goes back to the recruiters with the record they had, but with the source {@code support} (since
 * 2026-11-19), as in the sign-up ({@link VisBusinessReactivateRecruiterFromSignUp}, which also keeps the moment of the
 * return so the next e-mail of resumes does not inactivate them again). The recruiter also leaves the unsubscribed ones
 * ({@code vis_recruiter_unsubscribed}), when there.
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
	 * Reactivates the recruiter, with the source {@code support}, and ends the session telling the operator.
	 * @param json the json of the procedure, holding the inactive record under {@code _entities}
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code reactivated}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		CcpJsonRepresentation storedRecord = json.getInnerJsonFromPath(JsonFieldNames._entities, twinEntity);
		String supportSource = VisEntityRecruiter.Sources.support.getValue();
		CcpJsonRepresentation recordFromSupport = storedRecord.put(VisEntityRecruiter.Fields.source, supportSource);
		CcpJsonRepresentation jsonFromSupport = json.addToItem(JsonFieldNames._entities, twinEntity, recordFromSupport);
		CcpBusiness reactivate = new VisBusinessReactivateRecruiterFromSignUp(twinEntity, this.email);
		reactivate.execute(jsonFromSupport);
		// bringing the recruiter back is a will to receive the resumes again: the unsubscription goes away, as in a view
		// of a resume (VisBusinessReactivateRecruiterWhoViewed); a recruiter never unsubscribed makes this delete find nothing
		CcpJsonRepresentation unsubscribed = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterUnsubscribed.Fields.email, this.email);
		VisEntityRecruiterUnsubscribed.ENTITY.delete(unsubscribed);
		CcpJsonRepresentation sessionWithReply = VisSupportSaveRecruiterConversation.reply(this.session, VisSupportSaveRecruiterMessages.reactivated, this.email);
		return VisSupportSaveRecruiterStatus.reactivated.throwException(sessionWithReply);
	}
}
