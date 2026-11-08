package com.vis.business.recruiter.resumes;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;

/**
 * The table of the e-mail of the saved resume: the same table the recruiters receive, with the single row of the resume
 * just saved, as the invitation to the legacy candidates shows it ({@link VisBusinessBuildLegacyCandidateTable}). The row
 * has no signed link and no dates: the dates come from the history of the resume, which only a query reads, and the
 * save is not the place of a query (OQL).
 *
 * <p>Input: the saved resume, as {@code vis_resume} stores it, and {@code frontendUrl}. Output: the input with
 * {@code resumesTable}.
 */
public class VisBusinessBuildSavedResumeTable implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessBuildSavedResumeTable INSTANCE = new VisBusinessBuildSavedResumeTable();

	private VisBusinessBuildSavedResumeTable() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String frontendUrl = json.getAsString(VisRecruiterResumesFields.frontendUrl);
		JnLanguage language = VisBusinessBuildLegacyCandidateTable.LANGUAGE;
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, List.of(json));
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		CcpJsonRepresentation rowInput = json
				.put(VisRecruiterResumesFields.rankings, rankings)
				.put(JnJsonCommonsFields.language, language);
		CcpJsonRepresentation builtRow = VisBusinessBuildResumeRow.INSTANCE.execute(rowInput);
		String neverViewed = VisRecruiterResumesTexts.neverViewed.getMessage(language);
		String viewResume = VisRecruiterResumesTexts.viewResume.getMessage(language);
		CcpJsonRepresentation row = builtRow
				.put(VisRecruiterResumesFields.lastView, neverViewed)
				.put(VisRecruiterResumesFields.viewed, false)
				.put(VisRecruiterResumesFields.viewLabel, viewResume);
		String email = json.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.resumes, List.of(row))
				.put(JnJsonCommonsFields.email, email)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.language, language);
		CcpJsonRepresentation withTable = VisBusinessRenderResumesTable.INSTANCE.execute(tableInput);
		String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);
		CcpJsonRepresentation jsonWithTable = json.put(VisRecruiterResumesFields.resumesTable, resumesTable);
		return jsonWithTable;
	}
}
