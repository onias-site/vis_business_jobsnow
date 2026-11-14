package com.vis.business.bots.linkedin;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityInvalidLinkedinReport;

/**
 * Answers the report of a LinkedIn address: deletes it with the answer of the support ({@code supportResponse}), which
 * moves it, whole, to {@code vis_invalid_linkedin_report_solved}, where the recruiter sees it closed in the screen of
 * tickets. The report is the one found under {@code _entities} by the presence check that runs this action.
 */
public class VisSupportVerifyLinkedinAnswerTheReport implements CcpBusiness {

	/** The key of the report (readable e-mail of the recruiter and address). */
	private final CcpJsonRepresentation reportKey;

	/** The answer of the support. */
	private final String supportResponse;

	/**
	 * Keeps the key of the report and the answer.
	 * @param reportKey the key of the report, with the readable e-mail
	 * @param supportResponse the answer of the support, 10 to 500 characters
	 */
	public VisSupportVerifyLinkedinAnswerTheReport(CcpJsonRepresentation reportKey, String supportResponse) {
		this.reportKey = reportKey;
		this.supportResponse = supportResponse;
	}

	/**
	 * Answers the report.
	 * @param jsonWithTheStoredReport the json of the procedure, holding the report under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredReport) {
		CcpJsonRepresentation storedReport = jsonWithTheStoredReport.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntityInvalidLinkedinReport.ENTITY);
		// the stored e-mail is the hash: the readable one goes back for the twin, that receives the record as it is given
		CcpJsonRepresentation completeReport = storedReport.mergeWithAnotherJson(this.reportKey);
		CcpJsonRepresentation answeredReport = completeReport.put(VisEntityInvalidLinkedinReport.Fields.supportResponse, this.supportResponse);
		VisEntityInvalidLinkedinReport.ENTITY.delete(answeredReport);
		return jsonWithTheStoredReport;
	}
}
