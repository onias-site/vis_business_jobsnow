package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.vis.entities.VisEntityResumeDailyAction;
import com.vis.status.VisProcessStatusResumeDailyAction;

/**
 * Step of {@code VisServiceResume.Save} run when the resume already exists (active or inactive), so the save is an
 * update: the record of the update of the day ({@link VisEntityResumeDailyAction}), loaded by the first step of the
 * chain, placed under {@code _entities}, means the candidate already updated the resume in the last 24 hours, and the
 * flow ends with {@code dailyLimitReached} (429). Inserting a resume never gets here.
 */
public class VisBusinessRefuseResumeUpdateAlreadyRunToday implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisBusinessRefuseResumeUpdateAlreadyRunToday INSTANCE = new VisBusinessRefuseResumeUpdateAlreadyRunToday();

	private VisBusinessRefuseResumeUpdateAlreadyRunToday() {}

	/**
	 * Refuses the second update of the day.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the same json, when the resume was not updated today
	 * @throws CcpErrorFlowDisturb with {@code dailyLimitReached} when it was
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		CcpJsonRepresentation foundRecords = jsonAfterTheSteps.getInnerJson(CcpEntity.JsonFieldNames._entities);
		boolean notUpdatedToday = false == foundRecords.containsField(VisEntityResumeDailyAction.ENTITY);
		if(notUpdatedToday) {
			return jsonAfterTheSteps;
		}
		throw new CcpErrorFlowDisturb(jsonAfterTheSteps, VisProcessStatusResumeDailyAction.dailyLimitReached);
	}
}
