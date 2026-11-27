package com.vis.business.recruiter.resumes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityResume;

/**
 * The e-mail of resumes to a recruiter whose e-mail is in the text of a job posting of {@code /saveJobPosting} (since
 * 2026-11-26): the resumes found by the search of the job posting (the same ids of its link), the ones this recruiter may
 * see (active, not restricted, with a valid LinkedIn and not forbidding the company of the recruiter, as the screen of
 * the link shows them), in the usual e-mail of resumes to the recruiters ({@code VisNotifyRecruiterAboutResumes}, the
 * same table, subject and "see all"). As the e-mail of a single resume of a position, the dates of each resume are its
 * insertion and now, and no view is read. The list goes to {@code vis_recruiter_resumes}, so the recruiter gets no other
 * e-mail of resumes on the day ({@code VisBusinessSendResumesToRecruiters} skips who has the list of today).
 */
public class VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting implements CcpBusiness {

	/** The input of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The record of the recruiter, as stored in {@code vis_recruiter}. */
		recruiter,
		/** The ids of the resumes found (the hashes of their e-mails). */
		resumeIds,
		/** The output: whether the e-mail went. */
		sent,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting INSTANCE = new VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting();

	private VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting() {}

	/**
	 * Sends the e-mail.
	 * @param json the recruiter and the ids of the resumes
	 * @return {@code {"sent": <whether the e-mail went>}}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation recruiter = json.getInnerJson(Fields.recruiter);
		List<String> resumeIds = json.getAsStringList(Fields.resumeIds);
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		List<CcpJsonRepresentation> resumes = VisBusinessResumesOfThePositionFilters.getResumesToShow(resumeIds, email);
		boolean noResume = resumes.isEmpty();

		if(noResume) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.sent, false);
		}

		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		Map<String, long[]> resumeDates = getResumeDates(resumes);
		List<CcpJsonRepresentation> resumesOfTheEmail = VisBusinessSendResumesToRecruiters.getResumesOfTheEmail(resumes, rankings, resumeDates, Map.of());
		String emailHash = VisBusinessSendResumesToRecruiters.getEmailHash(email);
		List<CcpJsonRepresentation> rows = VisBusinessSendResumesToRecruiters.getRowsOfTheRecruiter(recruiter, emailHash, resumesOfTheEmail, Map.of(), Map.of(), Set.of());
		boolean noRow = rows.isEmpty();

		if(noRow) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.sent, false);
		}

		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		String visApiUrl = linkAddresses.getAsString(VisRecruiterResumesFields.visApiUrl);
		VisBusinessSendResumesToRecruiters.sendTheEmail(email, rows, frontendUrl, visApiUrl);
		return CcpOtherConstants.EMPTY_JSON.put(Fields.sent, true);
	}

	/**
	 * The insertion of each resume and now, by the hash of its e-mail.
	 * @param resumes the stored resumes
	 * @return the dates
	 */
	private static Map<String, long[]> getResumeDates(List<CcpJsonRepresentation> resumes) {
		long now = System.currentTimeMillis();
		Map<String, long[]> resumeDates = new HashMap<>();
		for (CcpJsonRepresentation resume : resumes) {
			String emailHash = resume.getAsString(VisEntityResume.Fields.email);
			long insertion = VisResumesOfThePositionSender.getInsertion(resume, now);
			resumeDates.put(emailHash, new long[] {insertion, now});
		}
		return resumeDates;
	}
}
