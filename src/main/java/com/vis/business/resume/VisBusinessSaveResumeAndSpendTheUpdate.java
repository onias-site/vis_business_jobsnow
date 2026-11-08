package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeDailyAction;
import com.vis.json.fields.validation.VisResumeDailyActions;

/**
 * Last step of {@code VisServiceResume.Save}: saves the resume and, when it already existed (found, active or inactive,
 * under {@code _entities}), spends the update of the day of the candidate ({@link VisEntityResumeDailyAction}).
 * Inserting a resume spends nothing.
 */
public class VisBusinessSaveResumeAndSpendTheUpdate implements CcpBusiness {

	/** The resume, as it came (with the session values). */
	private final CcpJsonRepresentation resume;

	/**
	 * Keeps the resume.
	 * @param resume the resume, as it came
	 */
	public VisBusinessSaveResumeAndSpendTheUpdate(CcpJsonRepresentation resume) {
		this.resume = resume;
	}

	/**
	 * Saves the resume and, in an update, the daily record.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the json of the procedure
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		VisEntityResume.ENTITY.save(this.resume);
		CcpJsonRepresentation foundRecords = jsonAfterTheSteps.getInnerJson(CcpEntity.JsonFieldNames._entities);
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		boolean isUpdate = foundRecords.containsField(VisEntityResume.ENTITY) || foundRecords.containsField(twinEntity);
		boolean isInsertion = false == isUpdate;
		if(isInsertion) {
			return jsonAfterTheSteps;
		}
		// written here, and not by the resume entity, because that one writes through the messaging: the next request
		// of the candidate must already find the day spent
		String email = this.resume.getAsString(VisEntityResume.Fields.email);
		CcpJsonRepresentation dailyActionKey = VisEntityResumeDailyAction.getKey(email, VisResumeDailyActions.update);
		VisEntityResumeDailyAction.ENTITY.save(dailyActionKey);
		return jsonAfterTheSteps;
	}
}
