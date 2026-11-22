package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeRestriction;
import com.vis.entities.VisEntityViewedResume;
import com.vis.entities.VisEntityViewedResumesOfTheMonth;

/**
 * The resumes that satisfy the filters of a position, listed to the recruiter logged in (link "Ver quais são estes
 * currículos" of the position form, since 2026-11-22), as the rows of the screen of the resumes of the week. The input
 * is the ids the count of the filters returned ({@code POST positions/resumes-of-the-filters}: the e-mails of the
 * resumes, hashed as stored), so the filters do not run twice: the resumes are read by those keys (mget, MGIE), together
 * with their restrictions, and the views of the recruiter are read in the group of every view of theirs (both sides).
 * Since the ids come from the browser, every rule of what the recruiter may see is checked again on each resume read.
 * Left out: the resumes no longer active, the ones with a restriction to correct, the ones whose LinkedIn the support
 * confirmed as invalid and the ones that forbid the company of the recruiter (the same rules of the link that opens the
 * resume). Each row brings the last view of the recruiter, the alias they gave (or "Ver currículo") and the signed link
 * that opens the resume and saves the view. The resumes the recruiter marked as unwanted (the view in the twin
 * {@code vis_inactive_viewed_resume}) go to a list of their own.
 */
public class VisBusinessResumesOfThePositionFilters implements CcpBusiness {

	/** The input and the output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The recruiter logged in. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		/** The ids returned by the count of the filters: the e-mails of the resumes, hashed as stored (SHA-1 with sign). */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 41)
		ids,

		/** The output: the rows of the resumes the recruiter did not mark as unwanted, viewed or not. */
		resumes,

		/** The output: the rows of the resumes the recruiter marked as unwanted. */
		unwantedResumes,
	}

	/** The single instance. */
	public static final VisBusinessResumesOfThePositionFilters INSTANCE = new VisBusinessResumesOfThePositionFilters();

	private VisBusinessResumesOfThePositionFilters() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String recruiterEmail = json.getAsString(Fields.email);
		List<String> emailHashes = json.getAsStringList(Fields.ids);
		List<CcpJsonRepresentation> resumes = getResumesToShow(emailHashes, recruiterEmail);

		CcpJsonRepresentation viewsQuery = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessViewedResumesOfTheMonth.Fields.email, recruiterEmail)
				.put(VisBusinessViewedResumesOfTheMonth.Fields.month, VisBusinessViewedResumesOfTheMonth.Constants.ALL_MONTHS.getValue());
		CcpJsonRepresentation views = VisBusinessViewedResumesOfTheMonth.INSTANCE.execute(viewsQuery);
		List<String> viewedTexts = views.getAsStringList(VisEntityViewedResumesOfTheMonth.Fields.viewedResume);
		List<String> unwantedTexts = views.getAsStringList(VisRecruiterResumesFields.unwantedResume);
		Map<String, CcpJsonRepresentation> viewByLinkedin = getViewByLinkedin(viewedTexts);
		Map<String, CcpJsonRepresentation> unwantedByLinkedin = getViewByLinkedin(unwantedTexts);

		List<CcpJsonRepresentation> rows = getRows(resumes);
		List<CcpJsonRepresentation> wantedRows = new ArrayList<>();
		List<CcpJsonRepresentation> unwantedRows = new ArrayList<>();
		for (CcpJsonRepresentation row : rows) {
			String linkedinHash = row.getAsString(VisEntityViewedResume.Fields.linkedin);
			boolean unwanted = unwantedByLinkedin.containsKey(linkedinHash);
			CcpJsonRepresentation view = unwanted ? unwantedByLinkedin.get(linkedinHash) : viewByLinkedin.getOrDefault(linkedinHash, CcpOtherConstants.EMPTY_JSON);
			CcpJsonRepresentation rowOfTheRecruiter = getRowOfTheRecruiter(row, view, recruiterEmail);
			List<CcpJsonRepresentation> target = unwanted ? unwantedRows : wantedRows;
			target.add(rowOfTheRecruiter);
		}
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON
				.put(Fields.resumes, wantedRows)
				.put(Fields.unwantedResumes, unwantedRows);
		return response;
	}

	/**
	 * The resumes of the e-mails (hashed) that can be shown to the recruiter, with their restrictions, in a single read.
	 * @param emailHashes the e-mails of the resumes, hashed as stored
	 * @param recruiterEmail the e-mail of the recruiter
	 * @return the resumes, as stored
	 */
	private static List<CcpJsonRepresentation> getResumesToShow(List<String> emailHashes, String recruiterEmail) {
		List<CcpJsonRepresentation> resumes = new ArrayList<>();

		if(emailHashes.isEmpty()) {
			return resumes;
		}

		List<CcpJsonRepresentation> resumeKeys = new ArrayList<>();
		for (String emailHash : emailHashes) {
			CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, emailHash);
			resumeKeys.add(resumeKey);
		}
		int keysCount = resumeKeys.size();
		CcpJsonRepresentation[] resumeKeysArray = resumeKeys.toArray(new CcpJsonRepresentation[keysCount]);
		// the restriction keeps the e-mail hashed as the resume does: read without the fields transformer
		CcpEntity restrictionByHashedEmail = CcpEntityFactory.getCustomEntity(VisEntityResumeRestriction.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll resumesById = crud.unionAll(resumeKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityResume.ENTITY, restrictionByHashedEmail);
		for (CcpJsonRepresentation resumeKey : resumeKeys) {
			boolean inactive = false == VisEntityResume.ENTITY.isPresentInThisUnionAll(resumesById, resumeKey);

			if(inactive) {
				continue;
			}

			boolean restricted = restrictionByHashedEmail.isPresentInThisUnionAll(resumesById, resumeKey);

			if(restricted) {
				continue;
			}

			Supplier<CcpJsonRepresentation> resumeKeySupplier = resumeKey.getJsonSupplier();
			CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRecordFromUnionAll(resumesById, resumeKeySupplier);
			boolean invalidLinkedinAddress = resume.getOrDefault(VisEntityResume.Fields.invalidLinkedinAddress, () -> false);

			if(invalidLinkedinAddress) {
				continue;
			}

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
	 * The rows of the resumes, in the order of the e-mail of resumes (type, CLT, PJ, the most experienced first), each one
	 * with the hash of its LinkedIn (the key of the views of the recruiter). No lines of insertion and last change: they come
	 * from the history, which only a query reads (OQL).
	 */
	private static List<CcpJsonRepresentation> getRows(List<CcpJsonRepresentation> resumes) {
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation resume : resumes) {
			CcpJsonRepresentation rowInput = resume
					.put(VisRecruiterResumesFields.rankings, rankings)
					.put(JnJsonCommonsFields.language, VisBusinessSendResumesToRecruiters.LANGUAGE);
			CcpJsonRepresentation row = VisBusinessBuildResumeRow.INSTANCE.execute(rowInput);
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			String linkedinHash = VisBusinessSendResumesToRecruiters.getLinkedinHash(linkedinAddress);
			CcpJsonRepresentation rowWithLinkedin = row.put(VisEntityViewedResume.Fields.linkedin, linkedinHash);
			rows.add(rowWithLinkedin);
		}
		rows.sort(VisResumesBySortKeys.INSTANCE);
		return rows;
	}

	/**
	 * The views of the recruiter by the hash of the LinkedIn of each one.
	 * @param viewTexts the views as the screen of the viewed resumes receives them (JSON texts with {@code linkedinHash})
	 * @return the views
	 */
	private static Map<String, CcpJsonRepresentation> getViewByLinkedin(List<String> viewTexts) {
		Map<String, CcpJsonRepresentation> viewByLinkedin = new HashMap<>();
		for (String viewText : viewTexts) {
			CcpJsonRepresentation view = new CcpJsonRepresentation(viewText);
			String linkedinHash = view.getAsString(VisRecruiterResumesFields.linkedinHash);
			viewByLinkedin.put(linkedinHash, view);
		}
		return viewByLinkedin;
	}

	/**
	 * The row as the screen of the resumes of the week shows it: the last view of the recruiter, whether they viewed it,
	 * the label of the link (their alias, or "Ver currículo") and the signed token of the link.
	 * @param row the row of the resume
	 * @param view the view of the recruiter, empty when they never viewed it
	 * @param recruiterEmail the e-mail of the recruiter
	 * @return the row to show
	 */
	private static CcpJsonRepresentation getRowOfTheRecruiter(CcpJsonRepresentation row, CcpJsonRepresentation view, String recruiterEmail) {
		boolean viewed = view.containsAllFields(VisRecruiterResumesFields.date);
		String lastViewLine = getLastViewLine(view, viewed);
		String viewResume = VisRecruiterResumesTexts.viewResume.getMessage(VisBusinessSendResumesToRecruiters.LANGUAGE);
		String viewLabel = view.getOrDefault(VisEntityViewedResume.Fields.alias, () -> viewResume);
		String resumeId = row.getAsString(VisRecruiterResumesFields.resumeId);
		String token = VisRecruiterLinks.view.sign(recruiterEmail, resumeId);
		CcpJsonRepresentation rowToShow = row
				.removeFields(VisRecruiterResumesFields.sortKeys, VisEntityViewedResume.Fields.linkedin)
				.put(VisRecruiterResumesFields.lastView, lastViewLine)
				.put(VisRecruiterResumesFields.viewed, viewed)
				.put(VisRecruiterResumesFields.viewLabel, viewLabel)
				.put(VisRecruiterResumesFields.token, token);
		return rowToShow;
	}

	private static String getLastViewLine(CcpJsonRepresentation view, boolean viewed) {

		if(false == viewed) {
			String neverViewed = VisRecruiterResumesTexts.neverViewed.getMessage(VisBusinessSendResumesToRecruiters.LANGUAGE);
			return neverViewed;
		}

		String date = view.getAsString(VisRecruiterResumesFields.date);
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.date, date);
		String lastViewLine = VisRecruiterResumesTexts.lastView.getMessage(VisBusinessSendResumesToRecruiters.LANGUAGE, values);
		return lastViewLine;
	}
}
