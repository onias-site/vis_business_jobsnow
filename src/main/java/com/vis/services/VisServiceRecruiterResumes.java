package com.vis.services;

import java.util.List;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.bulk.CcpErrorBulkEntityRecordNotFound;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.services.JnService;
import com.jn.utils.JnLanguage;
import com.vis.business.recruiter.resumes.VisBusinessDefaultResumeAlias;
import com.vis.business.recruiter.resumes.VisBusinessLoadSkillRankings;
import com.vis.business.recruiter.resumes.VisRecruiterLinks;
import com.vis.business.recruiter.resumes.VisBusinessViewedResumeDetails;
import com.vis.business.recruiter.resumes.VisBusinessViewedResumesOfTheMonth;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.entities.VisEntityInvalidLinkedinReport;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterResumes;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeRestriction;
import com.vis.entities.VisEntityViewedResume;
import com.vis.status.VisProcessStatusInvalidLinkedin;
import com.vis.status.VisProcessStatusViewedResume;
import com.vis.business.bots.linkedin.VisSupportVerifyLinkedinInactivateTheView;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;

/**
 * The actions of the recruiter that come from the e-mail of resumes: open a resume (and save the view), give it an
 * alias, stop receiving the e-mail (the three by a link signed for the recruiter, without a login) and see every resume
 * of the last e-mail (logged in). All of them read by id (OQL).
 */
public enum VisServiceRecruiterResumes implements JnService {
	/**
	 * Opens a resume of the e-mail: saves the view of the recruiter (with the alias they already gave to the resume, or
	 * the suggested one) and answers the LinkedIn of the resume, the alias and whether the view is inactive. A view in
	 * the twin ({@code vis_inactive_viewed_resume}, where the report of the LinkedIn puts it) is not saved again: the
	 * resume opens with the alias kept there, and the page does not let the view be saved (since 2026-11-13).
	 */
	ViewResumeFromEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resume = getResumeOfTheLink(json);
			String email = json.getAsString(JnJsonCommonsFields.email);
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			CcpJsonRepresentation view = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress)
					.put(VisEntityViewedResume.Fields.email, email);
			CcpJsonRepresentation storedViews = this.loadTheViews(view);
			CcpEntity twinEntity = VisEntityViewedResume.ENTITY.getTwinEntity();
			boolean inactiveView = storedViews.containsField(twinEntity);
			CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityResume.Fields.linkedinAddress, linkedinAddress)
					.put(VisRecruiterResumesFields.inactiveView, inactiveView);
			if(inactiveView) {
				CcpJsonRepresentation inactiveStoredView = storedViews.getInnerJson(twinEntity);
				String inactiveAlias = inactiveStoredView.getAsString(VisEntityViewedResume.Fields.alias);
				return response.put(VisEntityViewedResume.Fields.alias, inactiveAlias);
			}
			String alias = getAlias(storedViews, resume);
			CcpJsonRepresentation viewWithAlias = view.put(VisEntityViewedResume.Fields.alias, alias);
			CcpJsonRepresentation viewWithTheResume = putTheViewedResume(viewWithAlias, resume);
			VisEntityViewedResume.ENTITY.save(viewWithTheResume);
			CcpJsonRepresentation responseWithAlias = response.put(VisEntityViewedResume.Fields.alias, alias);
			return responseWithAlias;
		}

		/**
		 * The view of the recruiter on both sides, in one read: under the name of each entity where it is.
		 * @param view the key of the view, as typed
		 * @return the stored views, by entity
		 */
		private CcpJsonRepresentation loadTheViews(CcpJsonRepresentation view) {
			CcpEntity twinEntity = VisEntityViewedResume.ENTITY.getTwinEntity();
			CcpJsonRepresentation resultingData = new CcpGetEntityId(view)
			.toBeginProcedureAnd()
				.loadThisIdFromEntity(VisEntityViewedResume.ENTITY).and()
				.loadThisIdFromEntity(twinEntity)
				.andFinally(CcpEntity.JsonFieldNames._entities)
			.endThisProcedureRetrievingTheResultingData(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			boolean noView = false == resultingData.containsField(CcpEntity.JsonFieldNames._entities);
			if(noView) {
				return CcpOtherConstants.EMPTY_JSON;
			}
			CcpJsonRepresentation storedViews = resultingData.getInnerJson(CcpEntity.JsonFieldNames._entities);
			return storedViews;
		}

		/** The alias the recruiter already gave to the resume, or the suggested one when they never viewed it. */
		private String getAlias(CcpJsonRepresentation storedViews, CcpJsonRepresentation resume) {
			boolean viewed = storedViews.containsField(VisEntityViewedResume.ENTITY);
			if(viewed) {
				CcpJsonRepresentation savedView = storedViews.getInnerJson(VisEntityViewedResume.ENTITY);
				String savedAlias = savedView.getAsString(VisEntityViewedResume.Fields.alias);
				return savedAlias;
			}
			List<CcpJsonRepresentation> resumes = List.of(resume);
			CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
			CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
			CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
			CcpJsonRepresentation aliasInput = resume
					.put(VisRecruiterResumesFields.rankings, rankings)
					.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
			CcpJsonRepresentation withDefaultAlias = VisBusinessDefaultResumeAlias.INSTANCE.execute(aliasInput);
			String defaultAlias = withDefaultAlias.getAsString(VisRecruiterResumesFields.defaultAlias);
			return defaultAlias;
		}
	},
	/**
	 * Saves the alias the recruiter gave to a resume of the e-mail ("save view"). A view in the twin
	 * ({@code vis_inactive_viewed_resume}) is not saved: the save would take it out of there (since 2026-11-13).
	 */
	SaveResumeAliasFromEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resume = getResumeOfTheLink(json);
			String email = json.getAsString(JnJsonCommonsFields.email);
			String alias = json.getAsString(VisEntityViewedResume.Fields.alias);
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			CcpJsonRepresentation view = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress)
					.put(VisEntityViewedResume.Fields.email, email);
			CcpEntity twinEntity = VisEntityViewedResume.ENTITY.getTwinEntity();
			boolean inactiveView = twinEntity.exists(view);
			if(inactiveView) {
				return VisProcessStatusViewedResume.inactiveView.throwException(json);
			}
			CcpJsonRepresentation viewWithAlias = view.put(VisEntityViewedResume.Fields.alias, alias);
			CcpJsonRepresentation viewWithTheResume = putTheViewedResume(viewWithAlias, resume);
			VisEntityViewedResume.ENTITY.save(viewWithTheResume);
			CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(VisEntityViewedResume.Fields.alias, alias);
			return response;
		}
	},
	/**
	 * Reports that the LinkedIn address of a resume of the e-mail does not open (since 2026-11-12): saves the report in
	 * {@code vis_invalid_linkedin_report} (since 2026-11-13, a ticket of the recruiter in the screen of tickets), whose
	 * insert opens the ticket {@code /verifyLinkedinAddress <linkedinAddress> <resumeEmailSha1> <recruiterEmail>} in
	 * {@code /pendingTickets} of the support operator, who tells whether the recruiter is right. The view of the
	 * recruiter goes to the twin at once. The link must be valid, as to open the resume.
	 */
	ReportInvalidLinkedinFromEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resume = getResumeOfTheLink(json);
			String email = json.getAsString(JnJsonCommonsFields.email);
			String resumeId = json.getAsString(VisRecruiterResumesFields.resumeId);
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			CcpJsonRepresentation report = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityInvalidLinkedinReport.Fields.email, email)
					.put(VisEntityInvalidLinkedinReport.Fields.linkedinAddress, linkedinAddress)
					.put(VisEntityInvalidLinkedinReport.Fields.resumeEmailSha1, resumeId);
			// the insert sends the ticket to the operator; a report still waiting for the support changes nothing
			VisEntityInvalidLinkedinReport.ENTITY.save(report);
			// the view goes to the twin at once, with its comment (since 2026-11-13): the resume no longer goes to this
			// recruiter, whatever the support decides
			CcpJsonRepresentation view = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress)
					.put(VisEntityViewedResume.Fields.email, email);
			VisSupportVerifyLinkedinInactivateTheView.INSTANCE.execute(view);
			CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.linkedinAddress, linkedinAddress);
			return response;
		}
	},
	/** Stops the e-mails of resumes to the recruiter: moves them to the inactive recruiters ({@code vis_inactive_recruiter}). */
	UnsubscribeFromResumesEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String email = json.getAsString(JnJsonCommonsFields.email);
			String token = json.getAsString(VisRecruiterResumesFields.token);
			boolean invalidLink = false == VisRecruiterLinks.unsubscribe.matches(token, email);
			if(invalidLink) {
				throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.UNHAUTHORIZED);
			}
			CcpJsonRepresentation recruiter = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
			boolean active = VisEntityRecruiter.ENTITY.exists(recruiter);
			// a second click on the link finds the recruiter already inactive: the answer is the same
			if(active) {
				VisEntityRecruiter.ENTITY.delete(recruiter);
			}
			return recruiter;
		}
	},
	/** Every resume of the last e-mail of the recruiter logged in (the screen "see all"). */
	GetResumesOfTheWeek{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String email = json.getAsString(JnJsonCommonsFields.email);
			CcpJsonRepresentation recruiter = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterResumes.Fields.email, email);
			CcpJsonRepresentation recruiterResumes = VisEntityRecruiterResumes.ENTITY.getOneById(recruiter);
			List<CcpJsonRepresentation> resumes = recruiterResumes.getAsJsonList(VisEntityRecruiterResumes.Fields.resumes);
			CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterResumes.Fields.resumes, resumes);
			return response;
		}
	},
	/** The resumes the recruiter logged in viewed in a month (the views kept by every save of {@code vis_viewed_resume}). */
	GetViewedResumesOfTheMonth{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessViewedResumesOfTheMonth.INSTANCE.execute(json);
			return response;
		}
	},
	/** The details of a resume the recruiter logged in viewed in a month, as a row of the e-mail of resumes. */
	GetViewedResumeDetails{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessViewedResumeDetails.INSTANCE.execute(json);
			return response;
		}
	},
	;

	/**
	 * The view with the values of the viewed resume that the save keeps in the groups of its ddds: the ddds (stored in
	 * the view) and btc, clt, disponibility, experience, spokenLanguage, pcd, pj, resumeType, temporallyJobTime and travel
	 * (only in the child of each group), as the resume is stored.
	 * @param view the view to be saved
	 * @param resume the viewed resume, as stored
	 * @return the view with those values
	 */
	private static CcpJsonRepresentation putTheViewedResume(CcpJsonRepresentation view, CcpJsonRepresentation resume) {
		CcpJsonRepresentation viewedResume = resume.getJsonPiece(VisEntityResume.Fields.ddd, VisEntityResume.Fields.btc, VisEntityResume.Fields.clt,
				VisEntityResume.Fields.disponibility, VisEntityResume.Fields.experience, VisEntityResume.Fields.spokenLanguage,
				VisEntityResume.Fields.pcd, VisEntityResume.Fields.pj, VisEntityResume.Fields.resumeType,
				VisEntityResume.Fields.temporallyJobTime, VisEntityResume.Fields.travel, VisEntityResume.Fields.skill);
		CcpJsonRepresentation viewWithTheResume = view.mergeWithAnotherJson(viewedResume);
		return viewWithTheResume;
	}

	/**
	 * The resume of a signed link of the e-mail, read by its id. The companies the resume forbids are checked now, not
	 * only when the e-mail went out: a candidate who forbids the company of the recruiter after the e-mail closes the
	 * link. The answer is the same of a resume no longer active, so the recruiter does not learn they were forbidden.
	 * @throws CcpErrorFlowDisturb {@code 401} when the link was not signed for this recruiter and resume; {@code 404}
	 * when the resume is no longer active, now forbids the company of the recruiter or has a restriction its owner must
	 * correct ({@code vis_resume_restriction}); {@code 410} when the support
	 * confirmed that its LinkedIn address is invalid
	 */
	private static CcpJsonRepresentation getResumeOfTheLink(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonCommonsFields.email);
		String resumeId = json.getAsString(VisRecruiterResumesFields.resumeId);
		String token = json.getAsString(VisRecruiterResumesFields.token);
		boolean invalidLink = false == VisRecruiterLinks.view.matches(token, email, resumeId);
		if(invalidLink) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.UNHAUTHORIZED);
		}
		CcpJsonRepresentation resume = getActiveResume(json, resumeId);
		// the support confirmed that the LinkedIn address does not open (since 2026-11-12): no view is saved or counted
		boolean invalidLinkedinAddress = resume.getOrDefault(VisEntityResume.Fields.invalidLinkedinAddress, () -> false);
		if(invalidLinkedinAddress) {
			return VisProcessStatusInvalidLinkedin.invalidLinkedinAddress.throwException(json);
		}
		// the owner must correct the resume first (since 2026-11-12): the same answer of a resume no longer active. The
		// stored resume holds the e-mail hashed, as the restriction keeps it: read without the fields transformer
		CcpJsonRepresentation restrictionKey = resume.getJsonPiece(VisEntityResume.Fields.email);
		CcpEntity restrictionByHashedEmail = CcpEntityFactory.getCustomEntity(VisEntityResumeRestriction.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		boolean restricted = restrictionByHashedEmail.exists(restrictionKey);
		if(restricted) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
		List<String> notAllowedCompanies = resume.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
		boolean forbiddenCompany = VisBusinessSendResumesToRecruiters.isTheCompanyOfTheRecruiterForbidden(email, notAllowedCompanies);
		if(forbiddenCompany) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
		return resume;
	}

	/**
	 * The resume read by its id.
	 * @throws CcpErrorFlowDisturb {@code 404} when the resume is no longer active
	 */
	private static CcpJsonRepresentation getActiveResume(CcpJsonRepresentation json, String resumeId) {
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		String resumeEntityName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		try {
			CcpJsonRepresentation resume = crud.getOneById(resumeEntityName, resumeId);
			return resume;
		} catch (CcpErrorBulkEntityRecordNotFound resumeNoLongerActive) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
	}
}
