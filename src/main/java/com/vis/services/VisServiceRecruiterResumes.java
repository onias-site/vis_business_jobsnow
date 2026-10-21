package com.vis.services;

import java.util.List;

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
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterResumes;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityViewedResume;

/**
 * The actions of the recruiter that come from the e-mail of resumes: open a resume (and save the view), give it an
 * alias, stop receiving the e-mail (the three by a link signed for the recruiter, without a login) and see every resume
 * of the last e-mail (logged in). All of them read by id (OQL).
 */
public enum VisServiceRecruiterResumes implements JnService {
	/**
	 * Opens a resume of the e-mail: saves the view of the recruiter (with the alias they already gave to the resume, or
	 * the suggested one) and answers the LinkedIn of the resume and the alias.
	 */
	ViewResumeFromEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resume = getResumeOfTheLink(json);
			String email = json.getAsString(JnJsonCommonsFields.email);
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			CcpJsonRepresentation view = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress)
					.put(VisEntityViewedResume.Fields.email, email);
			String alias = getAlias(view, resume);
			CcpJsonRepresentation viewWithAlias = view.put(VisEntityViewedResume.Fields.alias, alias);
			VisEntityViewedResume.ENTITY.save(viewWithAlias);
			CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityResume.Fields.linkedinAddress, linkedinAddress)
					.put(VisEntityViewedResume.Fields.alias, alias);
			return response;
		}

		/** The alias the recruiter already gave to the resume, or the suggested one when they never viewed it. */
		private String getAlias(CcpJsonRepresentation view, CcpJsonRepresentation resume) {
			try {
				CcpJsonRepresentation savedView = VisEntityViewedResume.ENTITY.getOneById(view);
				String savedAlias = savedView.getAsString(VisEntityViewedResume.Fields.alias);
				return savedAlias;
			} catch (CcpErrorFlowDisturb neverViewed) {
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
		}
	},
	/** Saves the alias the recruiter gave to a resume of the e-mail ("save view"). */
	SaveResumeAliasFromEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resume = getResumeOfTheLink(json);
			String email = json.getAsString(JnJsonCommonsFields.email);
			String alias = json.getAsString(VisEntityViewedResume.Fields.alias);
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			CcpJsonRepresentation viewWithAlias = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress)
					.put(VisEntityViewedResume.Fields.email, email)
					.put(VisEntityViewedResume.Fields.alias, alias);
			VisEntityViewedResume.ENTITY.save(viewWithAlias);
			CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(VisEntityViewedResume.Fields.alias, alias);
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
	;

	/**
	 * The resume of a signed link of the e-mail, read by its id.
	 * @throws CcpErrorFlowDisturb {@code 401} when the link was not signed for this recruiter and resume; {@code 404}
	 * when the resume is no longer active
	 */
	private static CcpJsonRepresentation getResumeOfTheLink(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonCommonsFields.email);
		String resumeId = json.getAsString(VisRecruiterResumesFields.resumeId);
		String token = json.getAsString(VisRecruiterResumesFields.token);
		boolean invalidLink = false == VisRecruiterLinks.view.matches(token, email, resumeId);
		if(invalidLink) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.UNHAUTHORIZED);
		}
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
