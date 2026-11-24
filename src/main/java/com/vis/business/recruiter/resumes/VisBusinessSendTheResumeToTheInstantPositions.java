package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.position.VisBusinessActivePositionsOfTheFrequency;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionResumeSent;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisPositionFrequencyTypes;

/**
 * After every save of a resume (since 2026-11-24): the active positions that receive their resumes instantly and that the
 * resume satisfies get the e-mail "Cv recebido para a vaga" with this resume, once per pair position and resume
 * ({@code vis_position_resume_sent}, read for every position in a single union all). Runs in the queue listener of the
 * asynchronous writes of {@code vis_resume} (OQL). A resume whose LinkedIn address the support found invalid goes to no
 * position; the restriction to correct needs no check, since the save of the resume releases it.
 */
public class VisBusinessSendTheResumeToTheInstantPositions implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendTheResumeToTheInstantPositions INSTANCE = new VisBusinessSendTheResumeToTheInstantPositions();

	private VisBusinessSendTheResumeToTheInstantPositions() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation resume) {
		boolean invalidLinkedin = resume.containsAllFields(VisEntityResume.Fields.invalidLinkedinAddress) && resume.getAsBoolean(VisEntityResume.Fields.invalidLinkedinAddress);
		if(invalidLinkedin) {
			return resume;
		}
		String instantly = VisPositionFrequencyTypes.instantly.name();
		CcpJsonRepresentation positionsInput = CcpOtherConstants.EMPTY_JSON.put(VisBusinessActivePositionsOfTheFrequency.Fields.frequencyType, List.of(instantly));
		CcpJsonRepresentation positionsOutput = VisBusinessActivePositionsOfTheFrequency.INSTANCE.execute(positionsInput);
		List<CcpJsonRepresentation> positions = positionsOutput.getAsJsonList(VisBusinessActivePositionsOfTheFrequency.Fields.positions);
		List<CcpJsonRepresentation> resumeAsList = List.of(resume);
		List<CcpJsonRepresentation> matchingPositions = new ArrayList<>();
		for (CcpJsonRepresentation position : positions) {
			List<CcpJsonRepresentation> matchingResumes = VisBusinessSendResumesOfThePositionsOfThePeriod.getMatchingResumes(position, resumeAsList);
			boolean noMatch = matchingResumes.isEmpty();
			if(noMatch) {
				continue;
			}
			matchingPositions.add(position);
		}
		boolean nothingToSend = matchingPositions.isEmpty();
		if(nothingToSend) {
			return resume;
		}
		String emailHash = resume.getAsString(VisEntityResume.Fields.email);
		List<CcpJsonRepresentation> pairKeys = new ArrayList<>();
		for (CcpJsonRepresentation position : matchingPositions) {
			CcpJsonRepresentation pairKey = getPairKey(position, emailHash);
			pairKeys.add(pairKey);
		}
		int keysCount = pairKeys.size();
		CcpJsonRepresentation[] pairKeysArray = pairKeys.toArray(new CcpJsonRepresentation[keysCount]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll sentPairs = crud.unionAll(pairKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityPositionResumeSent.ENTITY);
		VisResumesOfThePositionSender sender = VisResumesOfThePositionSender.ofASavedResume(resume);
		for (int index = 0; index < keysCount; index++) {
			CcpJsonRepresentation pairKey = pairKeys.get(index);
			boolean alreadySent = VisEntityPositionResumeSent.ENTITY.isPresentInThisUnionAll(sentPairs, pairKey);
			if(alreadySent) {
				continue;
			}
			CcpJsonRepresentation position = matchingPositions.get(index);
			CcpJsonRepresentation sending = CcpOtherConstants.EMPTY_JSON
					.put(VisResumesOfThePositionSender.Fields.position, position)
					.put(VisResumesOfThePositionSender.Fields.resumes, resumeAsList);
			sender.accept(sending);
			VisEntityPositionResumeSent.ENTITY.save(pairKey);
		}
		return resume;
	}

	private static CcpJsonRepresentation getPairKey(CcpJsonRepresentation position, String emailHash) {
		String positionId = VisEntityPosition.ENTITY.calculateId(position);
		CcpJsonRepresentation pairKey = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPositionResumeSent.Fields.position, positionId)
				.put(VisEntityPositionResumeSent.Fields.resume, emailHash);
		return pairKey;
	}
}
