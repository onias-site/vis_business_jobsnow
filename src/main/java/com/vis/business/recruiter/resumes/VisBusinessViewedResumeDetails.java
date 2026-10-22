package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.entities.JnEntityGrouper;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLinkedin;
import com.vis.entities.VisEntityViewedResume;

/**
 * The details of a resume the recruiter viewed in a month, as a row of the e-mail of resumes
 * ({@code VisBusinessBuildResumeRow}, the same columns and texts), read by id (OQL): the group of the views of the
 * recruiter in the month and the e-mails that use the LinkedIn ({@code vis_resume_linkedin}) in a single read, then the
 * resumes of those e-mails ({@code vis_resume}) and the rankings of their skills. The row has no lines of insertion and
 * last change: they come from the history of the resume, which only a query reads.
 *
 * <p>Input: {@code email} (the recruiter), {@code month} ({@code yyyyMM}) and {@code linkedin} (as the list of the views
 * shows it). Output: {@code linkedin} and {@code resumes}, one row per active resume that uses the LinkedIn (usually one;
 * more when the LinkedIn is in the resume of more than one e-mail), empty when none is active anymore.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the LinkedIn is not among the views of the recruiter in the month: the
 * details are only of resumes the recruiter viewed
 */
public class VisBusinessViewedResumeDetails implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The recruiter. */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		email,

		/** The month of the view ({@code yyyyMM}). */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		month,

		/** The LinkedIn of the viewed resume. */
		@CcpJsonCopyFieldValidationsFrom(VisEntityViewedResume.Fields.class)
		@CcpJsonFieldValidatorRequired
		linkedin,
	}

	/** The single instance. */
	public static final VisBusinessViewedResumeDetails INSTANCE = new VisBusinessViewedResumeDetails();

	private VisBusinessViewedResumeDetails() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String linkedin = json.getAsString(Fields.linkedin);
		CcpJsonRepresentation groupKey = VisBusinessViewedResumesOfTheMonth.getGroupKeyOfTheMonth(json);
		CcpJsonRepresentation groupAndLinkedinKey = groupKey.put(VisEntityResumeLinkedin.Fields.linkedinAddress, linkedin);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll groupAndLinkedin = crud.unionAll(groupAndLinkedinKey, JnDeleteKeysFromCache.INSTANCE, JnEntityGrouper.ENTITY, VisEntityResumeLinkedin.ENTITY);

		Supplier<CcpJsonRepresentation> groupKeySupplier = groupKey.getJsonSupplier();
		CcpJsonRepresentation group = JnEntityGrouper.ENTITY.getRecordFromUnionAll(groupAndLinkedin, groupKeySupplier);
		List<String> views = group.getOrDefault(JnEntityGrouper.Fields.child, () -> List.of());
		boolean notViewed = false == wasViewed(views, linkedin);

		if(notViewed) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}

		Supplier<CcpJsonRepresentation> linkedinKeySupplier = groupAndLinkedinKey.getJsonSupplier();
		CcpJsonRepresentation linkedinRecord = VisEntityResumeLinkedin.ENTITY.getRecordFromUnionAll(groupAndLinkedin, linkedinKeySupplier);
		List<String> emailHashes = linkedinRecord.getOrDefault(VisEntityResumeLinkedin.Fields.emails, () -> List.of());
		List<CcpJsonRepresentation> resumes = getActiveResumes(crud, emailHashes);
		List<CcpJsonRepresentation> rows = getRows(resumes);
		CcpJsonRepresentation responseWithLinkedin = CcpOtherConstants.EMPTY_JSON.put(Fields.linkedin, linkedin);
		CcpJsonRepresentation response = responseWithLinkedin.put(VisRecruiterResumesFields.resumes, rows);
		return response;
	}

	/**
	 * Whether one of the views of the month (each a JSON text) is of the LinkedIn.
	 * @param views the views of the month
	 * @param linkedin the LinkedIn
	 * @return {@code true} when the recruiter viewed it in the month
	 */
	private static boolean wasViewed(List<String> views, String linkedin) {
		for (String viewText : views) {
			CcpJsonRepresentation view = new CcpJsonRepresentation(viewText);
			String viewedLinkedin = view.getOrDefault(VisEntityViewedResume.Fields.linkedin, () -> "");
			boolean sameLinkedin = linkedin.equals(viewedLinkedin);

			if(sameLinkedin) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The active resumes of the e-mails, in a single read.
	 * @param crud the crud
	 * @param emailHashes the hashes of the e-mails that use the LinkedIn
	 * @return the resumes found
	 */
	private static List<CcpJsonRepresentation> getActiveResumes(CcpCrud crud, List<String> emailHashes) {
		List<CcpJsonRepresentation> resumes = new ArrayList<>();
		boolean noEmail = emailHashes.isEmpty();

		if(noEmail) {
			return resumes;
		}

		List<CcpJsonRepresentation> resumeKeys = new ArrayList<>();
		for (String emailHash : emailHashes) {
			CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, emailHash);
			resumeKeys.add(resumeKey);
		}
		int keysCount = resumeKeys.size();
		CcpJsonRepresentation[] resumeKeysArray = resumeKeys.toArray(new CcpJsonRepresentation[keysCount]);
		CcpSelectUnionAll resumesById = crud.unionAll(resumeKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityResume.ENTITY);
		for (CcpJsonRepresentation resumeKey : resumeKeys) {
			boolean inactive = false == VisEntityResume.ENTITY.isPresentInThisUnionAll(resumesById, resumeKey);

			if(inactive) {
				continue;
			}

			Supplier<CcpJsonRepresentation> resumeKeySupplier = resumeKey.getJsonSupplier();
			CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRecordFromUnionAll(resumesById, resumeKeySupplier);
			resumes.add(resume);
		}
		return resumes;
	}

	/**
	 * The rows of the e-mail of resumes, with the rankings of the skills read by id.
	 * @param resumes the resumes
	 * @return the rows, without the keys that sort the e-mail
	 */
	private static List<CcpJsonRepresentation> getRows(List<CcpJsonRepresentation> resumes) {
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation resume : resumes) {
			CcpJsonRepresentation resumeWithRankings = resume.put(VisRecruiterResumesFields.rankings, rankings);
			CcpJsonRepresentation rowInput = resumeWithRankings.put(JnJsonCommonsFields.language, VisBusinessSendResumesToRecruiters.LANGUAGE);
			CcpJsonRepresentation row = VisBusinessBuildResumeRow.INSTANCE.execute(rowInput);
			CcpJsonRepresentation rowToShow = row.removeFields(VisRecruiterResumesFields.sortKeys);
			rows.add(rowToShow);
		}
		return rows;
	}
}
