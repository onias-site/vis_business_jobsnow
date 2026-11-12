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
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
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
 * <p>Input: {@code email} (the recruiter), {@code month} ({@code yyyyMM}) and {@code linkedinHash} (as the list of the
 * views shows it; the list never carries the LinkedIn). Output: {@code linkedinHash}, {@code resumes}, one row per active
 * resume that uses the LinkedIn, and {@code linkedin}, only when there is a row to show (usually one;
 * more when the LinkedIn is in the resume of more than one e-mail), empty when none is active anymore. A resume that now
 * forbids the company of the recruiter ({@code notAllowedCompany}) has no row, as if it were no longer active.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when no view of the recruiter in the month has a LinkedIn of this hash: the
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

		/** The hash of the LinkedIn of the viewed resume, as the list of the views shows it. */
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		@CcpJsonFieldValidatorRequired
		linkedinHash,
	}

	/** The single instance. */
	public static final VisBusinessViewedResumeDetails INSTANCE = new VisBusinessViewedResumeDetails();

	private VisBusinessViewedResumeDetails() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String linkedinHash = json.getAsString(Fields.linkedinHash);
		CcpJsonRepresentation groupKey = VisBusinessViewedResumesOfTheMonth.getGroupKeyOfTheMonth(json);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		// the LinkedIn comes from the view found in the group: the second read depends on it
		CcpSelectUnionAll groupById = crud.unionAll(groupKey, JnDeleteKeysFromCache.INSTANCE, JnEntityGrouper.ENTITY);
		Supplier<CcpJsonRepresentation> groupKeySupplier = groupKey.getJsonSupplier();
		CcpJsonRepresentation group = JnEntityGrouper.ENTITY.getRecordFromUnionAll(groupById, groupKeySupplier);
		List<String> views = group.getOrDefault(JnEntityGrouper.Fields.child, () -> List.of());
		String linkedin = getViewedLinkedin(views, linkedinHash, json);

		CcpJsonRepresentation linkedinKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeLinkedin.Fields.linkedinAddress, linkedin);
		CcpSelectUnionAll linkedinById = crud.unionAll(linkedinKey, JnDeleteKeysFromCache.INSTANCE, VisEntityResumeLinkedin.ENTITY);
		Supplier<CcpJsonRepresentation> linkedinKeySupplier = linkedinKey.getJsonSupplier();
		CcpJsonRepresentation linkedinRecord = VisEntityResumeLinkedin.ENTITY.getRecordFromUnionAll(linkedinById, linkedinKeySupplier);
		List<String> emailHashes = linkedinRecord.getOrDefault(VisEntityResumeLinkedin.Fields.emails, () -> List.of());
		String recruiterEmail = json.getAsString(Fields.email);
		List<CcpJsonRepresentation> resumes = getActiveResumes(crud, emailHashes, recruiterEmail);
		List<CcpJsonRepresentation> rows = getRows(resumes);
		CcpJsonRepresentation responseWithHash = CcpOtherConstants.EMPTY_JSON.put(Fields.linkedinHash, linkedinHash);
		CcpJsonRepresentation responseWithRows = responseWithHash.put(VisRecruiterResumesFields.resumes, rows);
		// no resume to show (none active, or every one forbids the company of the recruiter): the LinkedIn stays here
		boolean noResumeToShow = rows.isEmpty();
		if(noResumeToShow) {
			return responseWithRows;
		}
		CcpJsonRepresentation response = responseWithRows.put(VisEntityViewedResume.Fields.linkedin, linkedin);
		return response;
	}

	/**
	 * The LinkedIn of the view of the month (each a JSON text) whose hash is the given one.
	 * @param views the views of the month
	 * @param linkedinHash the hash of the LinkedIn
	 * @param json the input, for the error
	 * @return the LinkedIn
	 * @throws CcpErrorFlowDisturb {@code 404} when no view has a LinkedIn of this hash
	 */
	private static String getViewedLinkedin(List<String> views, String linkedinHash, CcpJsonRepresentation json) {
		for (String viewText : views) {
			CcpJsonRepresentation view = new CcpJsonRepresentation(viewText);
			String viewedLinkedin = view.getOrDefault(VisEntityViewedResume.Fields.linkedin, () -> "");
			boolean noLinkedin = viewedLinkedin.isEmpty();
			if(noLinkedin) {
				continue;
			}
			String viewedLinkedinHash = VisBusinessSendResumesToRecruiters.getLinkedinHash(viewedLinkedin);
			boolean sameLinkedin = linkedinHash.equals(viewedLinkedinHash);
			if(sameLinkedin) {
				return viewedLinkedin;
			}
		}
		throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
	}

	/**
	 * The active resumes of the e-mails that do not forbid the company of the recruiter, in a single read.
	 * @param crud the crud
	 * @param emailHashes the hashes of the e-mails that use the LinkedIn
	 * @param recruiterEmail the e-mail of the recruiter
	 * @return the resumes found
	 */
	private static List<CcpJsonRepresentation> getActiveResumes(CcpCrud crud, List<String> emailHashes, String recruiterEmail) {
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
			List<String> notAllowedCompanies = resume.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
			boolean forbiddenCompany = VisBusinessSendResumesToRecruiters.isTheCompanyOfTheRecruiterForbidden(recruiterEmail, notAllowedCompanies);

			if(forbiddenCompany) {
				continue;
			}

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
