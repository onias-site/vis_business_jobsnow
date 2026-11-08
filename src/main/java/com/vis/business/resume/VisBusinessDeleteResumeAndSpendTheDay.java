package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeDailyAction;
import com.vis.json.fields.validation.VisResumeDailyActions;

/**
 * Last step of {@code VisServiceResume.Delete}, once the candidate did not delete a resume in the last 24 hours:
 * deletes the resume for good, wherever it is, and spends the exclusion of the day ({@link VisEntityResumeDailyAction}).
 */
public class VisBusinessDeleteResumeAndSpendTheDay implements CcpBusiness {

	/** The key of the resume, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the key of the resume.
	 * @param json the key of the resume, as it came
	 */
	public VisBusinessDeleteResumeAndSpendTheDay(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Deletes the resume everywhere and saves the daily record.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the json of the procedure
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		VisEntityResume.ENTITY.deleteAnyWhere(this.json);
		String email = this.json.getAsString(VisEntityResume.Fields.email);
		CcpJsonRepresentation dailyActionKey = VisEntityResumeDailyAction.getKey(email, VisResumeDailyActions.exclusion);
		VisEntityResumeDailyAction.ENTITY.save(dailyActionKey);
		return jsonAfterTheSteps;
	}
}
