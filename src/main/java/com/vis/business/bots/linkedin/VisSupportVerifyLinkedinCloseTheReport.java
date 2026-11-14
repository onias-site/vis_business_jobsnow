package com.vis.business.bots.linkedin;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityInvalidLinkedinReport;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Closes the report of the recruiter ({@code vis_invalid_linkedin_report}) with the answer of the operator, in the
 * language of the recruiter: the report goes to the closed tickets of the screen of tickets
 * ({@link VisSupportVerifyLinkedinAnswerTheReport}). A ticket opened before the reports were stored (2026-11-13) has no
 * report, and nothing is closed.
 */
public class VisSupportVerifyLinkedinCloseTheReport implements CcpBusiness {

	/** The answer shown to the recruiter. */
	private final VisSupportVerifyLinkedinMessages answer;

	/**
	 * Keeps the answer.
	 * @param answer the answer shown to the recruiter
	 */
	public VisSupportVerifyLinkedinCloseTheReport(VisSupportVerifyLinkedinMessages answer) {
		this.answer = answer;
	}

	/** The field returned by the reading procedure, which answers nothing. */
	enum JsonFieldNames implements CcpJsonFieldName {
		/** The procedure returns no field. */
		inexistentField,
	}

	/**
	 * Closes the report, when there is one.
	 * @param json the session, with the parameters of the command
	 * @return the same session
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String linkedinAddress = json.getAsString(VisJsonCommonsFields.linkedinAddress);
		String recruiterEmail = json.getAsString(VisSupportVerifyLinkedinFields.recruiterEmail);
		CcpJsonRepresentation reportKey = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityInvalidLinkedinReport.Fields.email, recruiterEmail)
				.put(VisEntityInvalidLinkedinReport.Fields.linkedinAddress, linkedinAddress);
		CcpJsonRepresentation answerValues = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.linkedinAddress, linkedinAddress);
		JnLanguage language = VisSupportVerifyLinkedinConfirmValid.RECRUITER_LANGUAGE;
		String supportResponse = this.answer.getMessage(language, answerValues);
		CcpBusiness answerTheReport = new VisSupportVerifyLinkedinAnswerTheReport(reportKey, supportResponse);

		new CcpGetEntityId(reportKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityInvalidLinkedinReport.ENTITY).executeAction(answerTheReport)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}
}
