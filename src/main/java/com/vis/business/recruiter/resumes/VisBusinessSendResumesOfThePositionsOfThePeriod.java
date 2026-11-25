package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.position.VisBusinessActivePositionsOfTheFrequency;
import com.vis.business.position.VisBusinessFiltersOfThePosition;
import com.vis.business.position.VisBusinessResumeMatchesTheFilters;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisPositionFrequencyTypes;

/**
 * Sends each active position of a frequency (hourly, daily or weekly) the resumes that match it among the ones added or
 * changed in the period, in the order of the position (since 2026-11-24). The resumes of the period come from the history
 * of {@code vis_resume} (the same of the daily e-mail to every recruiter), without the invalid LinkedIn addresses and the
 * restricted resumes; each one is compared with the filters of each position in Java
 * ({@link VisBusinessResumeMatchesTheFilters}). The topics of the scheduled jobs are the subclasses, one per frequency.
 */
public abstract class VisBusinessSendResumesOfThePositionsOfThePeriod implements CcpBusiness {

	/** The frequency of the positions sent. */
	private final VisPositionFrequencyTypes frequency;

	/** The length of the period, in milliseconds. */
	private final long periodInMillis;

	/**
	 * Keeps the frequency and its period.
	 * @param frequency the frequency of the positions sent
	 * @param periodInMillis the length of the period, in milliseconds
	 */
	protected VisBusinessSendResumesOfThePositionsOfThePeriod(VisPositionFrequencyTypes frequency, long periodInMillis) {
		this.frequency = frequency;
		this.periodInMillis = periodInMillis;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String frequencyName = this.frequency.name();
		CcpJsonRepresentation positionsInput = CcpOtherConstants.EMPTY_JSON.put(VisBusinessActivePositionsOfTheFrequency.Fields.frequencyType, List.of(frequencyName));
		CcpJsonRepresentation positionsOutput = VisBusinessActivePositionsOfTheFrequency.INSTANCE.execute(positionsInput);
		List<CcpJsonRepresentation> positions = positionsOutput.getAsJsonList(VisBusinessActivePositionsOfTheFrequency.Fields.positions);
		int positionsFound = positions.size();
		CcpJsonRepresentation withPositions = json.put(VisRecruiterResumesFields.frequencyType, frequencyName).put(VisBusinessActivePositionsOfTheFrequency.Fields.positions, positionsFound);
		boolean noPosition = positions.isEmpty();
		if(noPosition) {
			return withPositions.put(VisRecruiterResumesFields.emailsSent, 0);
		}
		long since = System.currentTimeMillis() - this.periodInMillis;
		String resumeEntityName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		Set<String> changedResumes = VisBusinessSendResumesToRecruiters.getRecordsChangedSince(resumeEntityName, since, VisEntityResume.Fields.email);
		boolean noResume = changedResumes.isEmpty();
		if(noResume) {
			return withPositions.put(VisRecruiterResumesFields.resumesFound, 0).put(VisRecruiterResumesFields.emailsSent, 0);
		}
		List<CcpJsonRepresentation> resumes = VisBusinessSendResumesToRecruiters.getResumes(changedResumes);
		VisResumesOfThePositionSender sender = getSender(changedResumes);
		int emailsSent = 0;
		for (CcpJsonRepresentation position : positions) {
			List<CcpJsonRepresentation> matchingResumes = getMatchingResumes(position, resumes);
			boolean nothingForThePosition = matchingResumes.isEmpty();
			if(nothingForThePosition) {
				continue;
			}
			CcpJsonRepresentation sending = CcpOtherConstants.EMPTY_JSON
					.put(VisResumesOfThePositionSender.Fields.position, position)
					.put(VisResumesOfThePositionSender.Fields.resumes, matchingResumes);
			sender.accept(sending);
			emailsSent++;
		}
		int resumesFound = resumes.size();
		CcpJsonRepresentation counters = withPositions
				.put(VisRecruiterResumesFields.resumesFound, resumesFound)
				.put(VisRecruiterResumesFields.emailsSent, emailsSent);
		return counters;
	}

	// the views, the aliases and the dates are read once for every position of the run
	static VisResumesOfThePositionSender getSender(Set<String> changedResumes) {
		Map<String, long[]> resumeDates = VisBusinessSendResumesToRecruiters.getResumeDates(changedResumes);
		Map<String, Long> lastViewByPair = new HashMap<>();
		Map<String, Long> lastViewByRecruiter = new HashMap<>();
		VisBusinessSendResumesToRecruiters.loadViews(lastViewByPair, lastViewByRecruiter);
		Map<String, Long> viewsByResume = VisBusinessSendResumesToRecruiters.getViewsByResume(lastViewByPair);
		Map<String, String> aliasByPair = VisBusinessSendResumesToRecruiters.getAliases();
		Set<String> inactivePairs = VisBusinessSendResumesToRecruiters.getInactivePairs();
		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		VisResumesOfThePositionSender sender = new VisResumesOfThePositionSender(resumeDates, viewsByResume, lastViewByPair, aliasByPair, inactivePairs, frontendUrl);
		return sender;
	}

	/**
	 * The resumes that satisfy the filters of the position.
	 * @param position the position, as stored
	 * @param resumes the resumes, as stored
	 * @return the ones that match
	 */
	static List<CcpJsonRepresentation> getMatchingResumes(CcpJsonRepresentation position, List<CcpJsonRepresentation> resumes) {
		CcpJsonRepresentation filters = VisBusinessFiltersOfThePosition.INSTANCE.execute(position);
		List<CcpJsonRepresentation> matchingResumes = new ArrayList<>();
		for (CcpJsonRepresentation resume : resumes) {
			CcpJsonRepresentation matchInput = CcpOtherConstants.EMPTY_JSON
					.put(VisBusinessResumeMatchesTheFilters.Fields.filters, filters)
					.put(VisBusinessResumeMatchesTheFilters.Fields.resume, resume);
			CcpJsonRepresentation matchOutput = VisBusinessResumeMatchesTheFilters.INSTANCE.execute(matchInput);
			boolean matches = matchOutput.getAsBoolean(VisBusinessResumeMatchesTheFilters.Fields.matches);
			if(false == matches) {
				continue;
			}
			matchingResumes.add(resume);
		}
		return matchingResumes;
	}
}
