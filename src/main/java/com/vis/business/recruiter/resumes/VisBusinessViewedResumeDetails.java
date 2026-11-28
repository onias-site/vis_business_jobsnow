package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.compatibility.VisResumeRecruiterCompatibility;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLinkedin;
import com.vis.entities.VisEntityViewedResume;
import com.vis.json.transformers.VisResumeSkillsFromResumeChildField;

/**
 * The details of a resume the recruiter viewed in a month, as a row of the e-mail of resumes
 * ({@code VisBusinessBuildResumeRow}, the same columns and texts), read by id (OQL): the group of the views of the
 * recruiter in the month (on both sides of the grouper since 2026-11-15: the views of the twin too) and the e-mails that use the LinkedIn ({@code vis_resume_linkedin}) in a single read, then the
 * resumes of those e-mails ({@code vis_resume}) and the rankings of their skills. The row has no lines of insertion and
 * last change: they come from the history of the resume, which only a query reads.
 *
 * <p>Input: {@code email} (the recruiter), {@code month} ({@code yyyyMM} or {@code all}) and {@code linkedinHash} (as the list of the
 * views shows it; the list never carries the LinkedIn). Output: {@code linkedinHash}, {@code resumes}, one row per active
 * resume that uses the LinkedIn, each with the fields of the preview of the resume in {@code preview}, and {@code linkedin}, only when there is a row to show (usually one;
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

		/** The month of the view ({@code yyyyMM}), or {@code all} for the group of every view of the recruiter. */
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
		// the LinkedIn comes from the view found in the group (on both sides: the details also open the views moved to
		// the twin, tab "Indesejados"): the second read depends on it
		CcpJsonRepresentation view = VisBusinessViewedResumesOfTheMonth.getViewOfTheMonth(json);
		String linkedin = view.getAsString(VisEntityViewedResume.Fields.linkedin);

		String recruiterEmail = json.getAsString(Fields.email);
		List<CcpJsonRepresentation> resumes = getActiveResumesOfTheLinkedin(linkedin, recruiterEmail);
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
	 * The active resumes that use a LinkedIn and do not forbid the company of the recruiter: the e-mails that use the
	 * LinkedIn ({@code vis_resume_linkedin}) and then their resumes, two reads by id.
	 * @param linkedin the LinkedIn, as typed
	 * @param recruiterEmail the e-mail of the recruiter
	 * @return the resumes found, as stored
	 */
	static List<CcpJsonRepresentation> getActiveResumesOfTheLinkedin(String linkedin, String recruiterEmail) {
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpJsonRepresentation linkedinKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeLinkedin.Fields.linkedinAddress, linkedin);
		CcpSelectUnionAll linkedinById = crud.unionAll(linkedinKey, JnDeleteKeysFromCache.INSTANCE, VisEntityResumeLinkedin.ENTITY);
		Supplier<CcpJsonRepresentation> linkedinKeySupplier = linkedinKey.getJsonSupplier();
		CcpJsonRepresentation linkedinRecord = VisEntityResumeLinkedin.ENTITY.getRecordFromUnionAll(linkedinById, linkedinKeySupplier);
		List<String> emailHashes = linkedinRecord.getOrDefault(VisEntityResumeLinkedin.Fields.emails, () -> List.of());
		List<CcpJsonRepresentation> resumes = getActiveResumes(crud, emailHashes, recruiterEmail);
		return resumes;
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
			boolean forbiddenCompany = VisResumeRecruiterCompatibility.isTheCompanyOfTheRecruiterForbidden(recruiterEmail, notAllowedCompanies);

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
	 * @return the rows, without the keys that sort the e-mail, each with the preview of its resume
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
			CcpJsonRepresentation rowWithoutSortKeys = row.removeFields(VisRecruiterResumesFields.sortKeys);
			CcpJsonRepresentation preview = getPreview(resume);
			CcpJsonRepresentation rowToShow = rowWithoutSortKeys.put(VisRecruiterResumesFields.preview, preview);
			rows.add(rowToShow);
		}
		return rows;
	}

	/**
	 * The fields of the resume that the preview of the resume shows, as stored ({@code experience} is the year the
	 * experience started), without the e-mail, the companies the resume must not reach and the hashes; {@code skill}
	 * keeps only the names of the skills of the resume itself.
	 * @param resume the resume
	 * @return the preview
	 */
	public static CcpJsonRepresentation getPreview(CcpJsonRepresentation resume) {
		CcpJsonRepresentation fieldsToShow = resume.getJsonPiece(
				VisEntityResume.Fields.desiredJob,
				VisEntityResume.Fields.lastJob,
				VisEntityResume.Fields.linkedinAddress,
				VisEntityResume.Fields.resumeType,
				VisEntityResume.Fields.ddd,
				VisEntityResume.Fields.spokenLanguage,
				VisEntityResume.Fields.experience,
				VisEntityResume.Fields.disponibility,
				VisEntityResume.Fields.temporallyJobTime,
				VisEntityResume.Fields.travel,
				VisEntityResume.Fields.pcd,
				VisEntityResume.Fields.clt,
				VisEntityResume.Fields.pj,
				VisEntityResume.Fields.btc,
				VisEntityResume.Fields.negotiableClaim
		);
		CcpJsonRepresentation skillNames = VisResumeSkillsFromResumeChildField.INSTANCE.apply(resume);
		CcpJsonRepresentation preview = fieldsToShow.mergeWithAnotherJson(skillNames);
		return preview;
	}
}
