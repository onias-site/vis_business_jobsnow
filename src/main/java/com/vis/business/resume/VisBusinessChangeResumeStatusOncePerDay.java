package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpBusinessDeleteRecordFromEntity;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeDailyAction;
import com.vis.json.fields.validation.VisResumeDailyActions;
import com.vis.status.VisProcessStatusResumeDailyAction;

/**
 * Action of a branch of {@code VisServiceResume.ChangeStatus}: deactivates (delete on {@code vis_resume}) or
 * reactivates (delete on the twin) the resume, once a day. The candidate who already ran this action in the last 24
 * hours ({@link VisEntityResumeDailyAction}) gets {@code dailyLimitReached} (429) and nothing moves.
 * <p>
 * The daily check stays here, outside the chain of {@code ChangeStatus}: which action it is (and so the key of the
 * daily record) depends on the entity where the resume was found, a result of the same read.
 */
public class VisBusinessChangeResumeStatusOncePerDay implements CcpBusiness {

	/** The entity the resume is deleted from: the main one deactivates, the twin reactivates. */
	private final CcpEntity entity;

	/** The action this move is: {@code inactivation} or {@code reactivation}. */
	private final VisResumeDailyActions action;

	/** The key of the resume, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps what the move needs.
	 * @param entity the entity the resume is deleted from
	 * @param action the action this move is
	 * @param json the key of the resume, as it came
	 */
	public VisBusinessChangeResumeStatusOncePerDay(CcpEntity entity, VisResumeDailyActions action, CcpJsonRepresentation json) {
		this.entity = entity;
		this.action = action;
		this.json = json;
	}

	/**
	 * Refuses the action already run today, or moves the resume and spends the day of the action.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the json of the procedure
	 * @throws CcpErrorFlowDisturb with {@code dailyLimitReached} when the action was already run in the last 24 hours
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		String email = this.json.getAsString(VisEntityResume.Fields.email);
		CcpJsonRepresentation dailyActionKey = VisEntityResumeDailyAction.getKey(email, this.action);
		boolean alreadyRunToday = VisEntityResumeDailyAction.ENTITY.exists(dailyActionKey);
		if(alreadyRunToday) {
			throw new CcpErrorFlowDisturb(this.json, VisProcessStatusResumeDailyAction.dailyLimitReached);
		}
		CcpBusiness move = new CcpBusinessDeleteRecordFromEntity(this.entity, this.json);
		CcpJsonRepresentation result = move.execute(jsonAfterTheSteps);
		// written here, and not after the move by the resume entity, because that one writes through the messaging: the
		// next request of the candidate must already find the day spent
		VisEntityResumeDailyAction.ENTITY.save(dailyActionKey);
		return result;
	}
}
