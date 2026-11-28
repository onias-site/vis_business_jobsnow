package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.vis.business.position.VisBusinessPositionsOpenToTheResume;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityPositionResumeSent;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualPositionInterestHour;
import com.vis.entities.VisEntityVirtualPositionInterestedResumes;

/**
 * The topic of every full hour (job {@code jobsnow-reenvio-token-login}, the first of its topics, since 2026-11-24): sends
 * each position the resumes of the candidates who asked, in the screen "Meus dados → Minhas vagas", to send their resume
 * to it before this hour ({@code vis_position_interest}), in the e-mail of the lists of resumes of the positions
 * ("{n} Cvs recebidos para a vaga"). Everything is read by id: the group of the hour gives the positions, the group of
 * each position and hour gives the resumes. The group of the hour before is read too, so an interest whose asynchronous
 * write reached the group after the run of its hour goes in the next one. Both sides are checked again
 * ({@link VisBusinessPositionsOpenToTheResume}), since the resume or the position may have changed since the interest:
 * a resume that became inactive, restricted or with the LinkedIn confirmed invalid, and a position that became inactive,
 * expired, of a company the resume forbids or of a recruiter who no longer wants the resume, are not sent. A pair
 * position and resume goes once ({@code vis_position_resume_sent}).
 */
public class VisBusinessSendTheResumesOfTheInterestedCandidates implements CcpBusiness {

	/** The output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The hours whose groups were read. */
		sendingHours,
		/** How many positions had interested candidates. */
		positionsWithInterests,
		/** How many resumes went to the recruiters. */
		resumesSent,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendTheResumesOfTheInterestedCandidates INSTANCE = new VisBusinessSendTheResumesOfTheInterestedCandidates();

	private VisBusinessSendTheResumesOfTheInterestedCandidates() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		long now = System.currentTimeMillis();
		long oneHourAgo = now - VisEntityVirtualPositionInterestHour.ONE_HOUR;
		String previousHour = VisEntityVirtualPositionInterestHour.getSendingHour(oneHourAgo);
		String currentHour = VisEntityVirtualPositionInterestHour.getSendingHour(now);
		List<String> sendingHours = List.of(previousHour, currentHour);
		Map<String, Set<String>> resumeIdsByPosition = getInterestedResumes(sendingHours);
		int positionsWithInterests = resumeIdsByPosition.size();
		CcpJsonRepresentation withHours = json
				.put(Fields.sendingHours, sendingHours)
				.put(Fields.positionsWithInterests, positionsWithInterests);
		boolean noInterest = resumeIdsByPosition.isEmpty();
		if(noInterest) {
			return withHours.put(Fields.resumesSent, 0).put(VisRecruiterResumesFields.emailsSent, 0);
		}
		Set<String> positionIds = resumeIdsByPosition.keySet();
		CcpJsonRepresentation positionsById = getPositions(positionIds);
		Map<String, List<CcpJsonRepresentation>> resumesByPosition = getOpenResumesByPosition(resumeIdsByPosition, positionsById);
		Map<String, List<CcpJsonRepresentation>> resumesToSend = withoutTheResumesAlreadySent(resumesByPosition);
		boolean nothingToSend = resumesToSend.isEmpty();
		if(nothingToSend) {
			return withHours.put(Fields.resumesSent, 0).put(VisRecruiterResumesFields.emailsSent, 0);
		}
		Set<String> emailHashes = new LinkedHashSet<>();
		for (List<CcpJsonRepresentation> resumes : resumesToSend.values()) {
			for (CcpJsonRepresentation resume : resumes) {
				String emailHash = resume.getAsString(VisEntityResume.Fields.email);
				emailHashes.add(emailHash);
			}
		}
		VisResumesOfThePositionSender sender = VisBusinessSendResumesOfThePositionsOfThePeriod.getSender(emailHashes);
		int resumesSent = 0;
		for (Map.Entry<String, List<CcpJsonRepresentation>> entry : resumesToSend.entrySet()) {
			String positionId = entry.getKey();
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = positionsById.getInnerJson(positionIdField);
			List<CcpJsonRepresentation> resumes = entry.getValue();
			CcpJsonRepresentation sending = CcpOtherConstants.EMPTY_JSON
					.put(VisResumesOfThePositionSender.Fields.position, position)
					.put(VisResumesOfThePositionSender.Fields.resumes, resumes);
			sender.accept(sending);
			for (CcpJsonRepresentation resume : resumes) {
				CcpJsonRepresentation pairKey = getPairKey(positionId, resume);
				VisEntityPositionResumeSent.ENTITY.save(pairKey);
			}
			int resumesOfThePosition = resumes.size();
			resumesSent += resumesOfThePosition;
		}
		int emailsSent = resumesToSend.size();
		CcpJsonRepresentation counters = withHours
				.put(Fields.resumesSent, resumesSent)
				.put(VisRecruiterResumesFields.emailsSent, emailsSent);
		return counters;
	}

	// the group of each hour names the positions; the group of each position and hour names the resumes
	private static Map<String, Set<String>> getInterestedResumes(List<String> sendingHours) {
		String childName = VisEntityPositionInterest.ENTITY.getEntityMetaData().entityName;
		String hourParentName = VisEntityVirtualPositionInterestHour.ENTITY.getEntityMetaData().entityName;
		String resumesParentName = VisEntityVirtualPositionInterestedResumes.ENTITY.getEntityMetaData().entityName;
		List<CcpJsonRepresentation> hourGroupKeys = new ArrayList<>();
		for (String sendingHour : sendingHours) {
			String hourParentId = VisEntityVirtualPositionInterestHour.getParentId(sendingHour);
			CcpJsonRepresentation hourGroupKey = JnGroupers.getGroupKey(hourParentName, childName, hourParentId);
			hourGroupKeys.add(hourGroupKey);
		}
		List<List<String>> positionsOfEachHour = JnGroupers.getChildrenOfEachGroup(hourGroupKeys);
		List<String> positionOfEachGroup = new ArrayList<>();
		List<CcpJsonRepresentation> resumesGroupKeys = new ArrayList<>();
		for (int index = 0; index < sendingHours.size(); index++) {
			String sendingHour = sendingHours.get(index);
			List<String> positionIds = positionsOfEachHour.get(index);
			for (String positionId : positionIds) {
				String resumesParentId = VisEntityVirtualPositionInterestedResumes.getParentId(positionId, sendingHour);
				CcpJsonRepresentation resumesGroupKey = JnGroupers.getGroupKey(resumesParentName, childName, resumesParentId);
				resumesGroupKeys.add(resumesGroupKey);
				positionOfEachGroup.add(positionId);
			}
		}
		Map<String, Set<String>> resumeIdsByPosition = new LinkedHashMap<>();
		boolean noPosition = resumesGroupKeys.isEmpty();
		if(noPosition) {
			return resumeIdsByPosition;
		}
		List<List<String>> resumesOfEachGroup = JnGroupers.getChildrenOfEachGroup(resumesGroupKeys);
		for (int index = 0; index < resumesGroupKeys.size(); index++) {
			String positionId = positionOfEachGroup.get(index);
			List<String> resumeIds = resumesOfEachGroup.get(index);
			Set<String> resumeIdsOfThePosition = resumeIdsByPosition.computeIfAbsent(positionId, id -> new LinkedHashSet<>());
			resumeIdsOfThePosition.addAll(resumeIds);
		}
		return resumeIdsByPosition;
	}

	private static CcpJsonRepresentation getPositions(Set<String> positionIds) {
		CcpEntityMetaData positionDetails = VisEntityPosition.ENTITY.getEntityMetaData();
		CcpJsonRepresentation positionsById = positionDetails.getMultipleByRecordIds(positionIds);
		return positionsById;
	}

	// each active resume with the positions it is interested in that are still open to it, both sides checked again
	private static Map<String, List<CcpJsonRepresentation>> getOpenResumesByPosition(Map<String, Set<String>> resumeIdsByPosition, CcpJsonRepresentation positionsById) {
		Map<String, Set<String>> positionIdsByResume = new LinkedHashMap<>();
		for (Map.Entry<String, Set<String>> entry : resumeIdsByPosition.entrySet()) {
			String positionId = entry.getKey();
			for (String resumeId : entry.getValue()) {
				Set<String> positionIdsOfTheResume = positionIdsByResume.computeIfAbsent(resumeId, id -> new LinkedHashSet<>());
				positionIdsOfTheResume.add(positionId);
			}
		}
		Set<String> resumeIds = positionIdsByResume.keySet();
		CcpEntityMetaData resumeDetails = VisEntityResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation resumesById = resumeDetails.getMultipleByRecordIds(resumeIds);
		Map<String, List<CcpJsonRepresentation>> resumesByPosition = new LinkedHashMap<>();
		for (Map.Entry<String, Set<String>> entry : positionIdsByResume.entrySet()) {
			String resumeId = entry.getKey();
			CcpFieldName resumeIdField = new CcpFieldName(resumeId);
			CcpJsonRepresentation resume = resumesById.getInnerJson(resumeIdField);
			boolean inactiveResume = resume.isEmpty();
			if(inactiveResume) {
				continue;
			}
			CcpJsonRepresentation positionsOfTheResume = CcpOtherConstants.EMPTY_JSON;
			for (String positionId : entry.getValue()) {
				CcpFieldName positionIdField = new CcpFieldName(positionId);
				CcpJsonRepresentation position = positionsById.getInnerJson(positionIdField);
				positionsOfTheResume = positionsOfTheResume.put(positionIdField, position);
			}
			CcpJsonRepresentation openInput = CcpOtherConstants.EMPTY_JSON
					.put(VisBusinessPositionsOpenToTheResume.Fields.resume, resume)
					.put(VisBusinessPositionsOpenToTheResume.Fields.positions, positionsOfTheResume);
			CcpJsonRepresentation openOutput = VisBusinessPositionsOpenToTheResume.INSTANCE.execute(openInput);
			CcpJsonRepresentation openPositions = openOutput.getInnerJson(VisBusinessPositionsOpenToTheResume.Fields.positions);
			Set<String> openPositionIds = openPositions.fieldSet();
			for (String positionId : openPositionIds) {
				List<CcpJsonRepresentation> resumesOfThePosition = resumesByPosition.computeIfAbsent(positionId, id -> new ArrayList<>());
				resumesOfThePosition.add(resume);
			}
		}
		return resumesByPosition;
	}

	// a pair position and resume already sent (instantly, by an earlier hour or by another interest) is not sent again
	private static Map<String, List<CcpJsonRepresentation>> withoutTheResumesAlreadySent(Map<String, List<CcpJsonRepresentation>> resumesByPosition) {
		List<CcpJsonRepresentation> pairKeys = new ArrayList<>();
		for (Map.Entry<String, List<CcpJsonRepresentation>> entry : resumesByPosition.entrySet()) {
			String positionId = entry.getKey();
			for (CcpJsonRepresentation resume : entry.getValue()) {
				CcpJsonRepresentation pairKey = getPairKey(positionId, resume);
				pairKeys.add(pairKey);
			}
		}
		Map<String, List<CcpJsonRepresentation>> resumesToSend = new LinkedHashMap<>();
		boolean noPair = pairKeys.isEmpty();
		if(noPair) {
			return resumesToSend;
		}
		int keysCount = pairKeys.size();
		CcpJsonRepresentation[] pairKeysArray = pairKeys.toArray(new CcpJsonRepresentation[keysCount]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll sentPairs = crud.unionAll(pairKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityPositionResumeSent.ENTITY);
		for (Map.Entry<String, List<CcpJsonRepresentation>> entry : resumesByPosition.entrySet()) {
			String positionId = entry.getKey();
			for (CcpJsonRepresentation resume : entry.getValue()) {
				CcpJsonRepresentation pairKey = getPairKey(positionId, resume);
				boolean alreadySent = VisEntityPositionResumeSent.ENTITY.isPresentInThisUnionAll(sentPairs, pairKey);
				if(alreadySent) {
					continue;
				}
				List<CcpJsonRepresentation> resumesOfThePosition = resumesToSend.computeIfAbsent(positionId, id -> new ArrayList<>());
				resumesOfThePosition.add(resume);
			}
		}
		return resumesToSend;
	}

	private static CcpJsonRepresentation getPairKey(String positionId, CcpJsonRepresentation resume) {
		String emailHash = resume.getAsString(VisEntityResume.Fields.email);
		CcpJsonRepresentation pairKey = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPositionResumeSent.Fields.position, positionId)
				.put(VisEntityPositionResumeSent.Fields.resume, emailHash);
		return pairKey;
	}
}
