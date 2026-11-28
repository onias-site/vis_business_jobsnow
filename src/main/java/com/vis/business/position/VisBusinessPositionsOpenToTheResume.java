package com.vis.business.position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.compatibility.VisResumeRecruiterCompatibility;
import com.vis.business.compatibility.VisResumeRecruiterFacts;
import com.vis.business.compatibility.VisResumeRecruiterFactsByIds;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisPositionFrequencyTypes;
import com.vis.json.transformers.VisPositionPublishingDayCombinations;

/**
 * The positions open to a resume (since 2026-11-24), on both sides: the candidate sees, and sends their resume to, only
 * these positions, and the recruiter receives only these resumes. A position is open to the resume when it:
 * <ul>
 * <li>exists in {@code vis_position} (an inactive or deleted position is not among the stored ones);</li>
 * <li>receives the resumes from the candidates themselves ({@code frequencyType} {@code byTheCandidates});</li>
 * <li>is not expired;</li>
 * <li>has filters that the resume satisfies ({@link VisBusinessResumeMatchesTheFilters});</li>
 * <li>is compatible with the resume, both the recruiter who owns it and the e-mail that receives its resumes
 * ({@code resumesEmail}), by every rule between recruiter and resume ({@link VisResumeRecruiterCompatibility}, since
 * 2026-11-28 the single place of those rules: the unsubscription, the company forbidden, the view in the twin
 * {@code vis_inactive_viewed_resume}).</li>
 * </ul>
 * A resume blocked by a rule of its own (a restriction to correct, the LinkedIn confirmed invalid) has no position open,
 * and the output tells it ({@link Fields#blocked}). Reads by id only (OQL): the facts of every pair in one read.
 */
public class VisBusinessPositionsOpenToTheResume implements CcpBusiness {

	/** The input and the output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The resume, as stored. */
		resume,
		/** The input: the positions, as stored, by id. The output: only the open ones, by id. */
		positions,
		/** The output: whether a rule of the resume alone blocks it (the owner must correct it). */
		blocked,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessPositionsOpenToTheResume INSTANCE = new VisBusinessPositionsOpenToTheResume();

	private VisBusinessPositionsOpenToTheResume() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation storedResume = json.getInnerJson(Fields.resume);
		CcpJsonRepresentation positionsById = json.getInnerJson(Fields.positions);
		long now = System.currentTimeMillis();
		Map<String, CcpJsonRepresentation> candidates = new HashMap<>();
		Map<String, List<CcpJsonRepresentation>> recruitersByPosition = new HashMap<>();
		List<CcpJsonRepresentation> recruiters = new ArrayList<>();
		Set<String> positionIds = positionsById.fieldSet();
		for (String positionId : positionIds) {
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = positionsById.getInnerJson(positionIdField);
			boolean closed = isClosed(position, storedResume, now);
			if(closed) {
				continue;
			}
			candidates.put(positionId, position);
			List<CcpJsonRepresentation> recruitersOfThePosition = getRecruitersOfThePosition(position);
			recruitersByPosition.put(positionId, recruitersOfThePosition);
			recruiters.addAll(recruitersOfThePosition);
		}
		CcpJsonRepresentation resume = VisResumeRecruiterCompatibility.getResume(storedResume);
		List<CcpJsonRepresentation> resumes = List.of(resume);
		VisResumeRecruiterFacts facts = VisResumeRecruiterFactsByIds.load(recruiters, resumes);
		boolean blocked = VisResumeRecruiterCompatibility.isTheResumeBlocked(resume, facts);
		if(blocked) {
			return json.put(Fields.positions, CcpOtherConstants.EMPTY_JSON).put(Fields.blocked, true);
		}
		CcpJsonRepresentation openPositions = CcpOtherConstants.EMPTY_JSON;
		for (Map.Entry<String, CcpJsonRepresentation> candidate : candidates.entrySet()) {
			String positionId = candidate.getKey();
			List<CcpJsonRepresentation> recruitersOfThePosition = recruitersByPosition.get(positionId);
			boolean compatible = recruitersOfThePosition.stream().allMatch(recruiter -> VisResumeRecruiterCompatibility.mayReceive(recruiter, resume, facts));
			if(false == compatible) {
				continue;
			}
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = candidate.getValue();
			openPositions = openPositions.put(positionIdField, position);
		}
		CcpJsonRepresentation response = json
				.put(Fields.positions, openPositions)
				.put(Fields.blocked, false);
		return response;
	}

	/**
	 * Whether the validity of the position is over.
	 * @param position the position, as stored
	 * @param now the current time
	 * @return {@code true} when the position is expired
	 */
	public static boolean isExpired(CcpJsonRepresentation position, long now) {
		CcpJsonRepresentation expiration = position.getInnerJson(VisEntityPosition.Fields.expiration);
		boolean noEnd = false == expiration.containsAllFields(VisPositionPublishingDayCombinations.ExpirationFields.max);
		if (noEnd) {
			return false;
		}
		Double end = expiration.getAsDoubleNumber(VisPositionPublishingDayCombinations.ExpirationFields.max);
		boolean expired = end.longValue() < now;
		return expired;
	}

	// a position read by id may be gone, of another frequency, expired or asking what the resume does not have
	private static boolean isClosed(CcpJsonRepresentation position, CcpJsonRepresentation resume, long now) {
		boolean notFound = position.isEmpty();
		if (notFound) {
			return true;
		}
		String frequencyType = position.getOrDefault(VisEntityPosition.Fields.frequencyType, () -> "");
		boolean byTheCandidates = VisPositionFrequencyTypes.byTheCandidates.name().equals(frequencyType);
		if (false == byTheCandidates) {
			return true;
		}
		boolean expired = isExpired(position, now);
		if (expired) {
			return true;
		}
		CcpJsonRepresentation filters = VisBusinessFiltersOfThePosition.INSTANCE.execute(position);
		CcpJsonRepresentation matchInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessResumeMatchesTheFilters.Fields.filters, filters)
				.put(VisBusinessResumeMatchesTheFilters.Fields.resume, resume);
		CcpJsonRepresentation matchOutput = VisBusinessResumeMatchesTheFilters.INSTANCE.execute(matchInput);
		boolean matches = matchOutput.getAsBoolean(VisBusinessResumeMatchesTheFilters.Fields.matches);
		return false == matches;
	}

	// the recruiter who owns the position (the hash of the e-mail and the domain are kept in it) and the e-mail that
	// receives its resumes
	private static List<CcpJsonRepresentation> getRecruitersOfThePosition(CcpJsonRepresentation position) {
		List<CcpJsonRepresentation> recruiters = new ArrayList<>();
		String ownerEmailHash = position.getAsString(VisEntityPosition.Fields.recruiter);
		String ownerDomain = position.getOrDefault(VisEntityPosition.Fields.recruiterDomain, () -> "");
		CcpJsonRepresentation owner = VisResumeRecruiterCompatibility.getRecruiterOfTheDomain(ownerEmailHash, ownerDomain);
		recruiters.add(owner);
		String resumesEmail = position.getOrDefault(VisEntityPosition.Fields.resumesEmail, () -> "");
		boolean noRecipient = resumesEmail.isEmpty();
		if (noRecipient) {
			return recruiters;
		}
		CcpJsonRepresentation recipient = VisResumeRecruiterCompatibility.getRecruiter(resumesEmail);
		recruiters.add(recipient);
		return recruiters;
	}
}
