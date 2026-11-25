package com.vis.business.position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityViewedResume;
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
 * <li>is not of a company the resume forbids ({@code notAllowedCompany}): neither the domain of the recruiter who owns it
 * ({@code recruiterDomain}) nor the domain of the e-mail that receives its resumes ({@code resumesEmail});</li>
 * <li>is not of a recruiter who does not want the resume: the view of the resume by the recruiter, or by the e-mail that
 * receives the resumes, is in the twin {@code vis_inactive_viewed_resume} ("Negativar CV" or the report of the LinkedIn).</li>
 * </ul>
 * The resume itself must not be blocked: one with the LinkedIn address confirmed invalid or with a restriction to correct
 * ({@code vis_resume_restriction}) has no position open; the caller tells which ({@link Fields#restricted}).
 * Reads by id only (OQL): the views of the twin, in one read for every position.
 */
public class VisBusinessPositionsOpenToTheResume implements CcpBusiness {

	/** The input and the output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The resume, as stored. */
		resume,
		/** Whether the resume has a restriction to correct ({@code vis_resume_restriction}). */
		restricted,
		/** The input: the positions, as stored, by id. The output: only the open ones, by id. */
		positions,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessPositionsOpenToTheResume INSTANCE = new VisBusinessPositionsOpenToTheResume();

	private VisBusinessPositionsOpenToTheResume() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation resume = json.getInnerJson(Fields.resume);
		boolean restricted = json.getAsBoolean(Fields.restricted);
		boolean blocked = restricted || hasTheLinkedinConfirmedInvalid(resume);
		if(blocked) {
			return json.put(Fields.positions, CcpOtherConstants.EMPTY_JSON);
		}
		CcpJsonRepresentation positionsById = json.getInnerJson(Fields.positions);
		long now = System.currentTimeMillis();
		List<String> companies = resume.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
		Map<String, CcpJsonRepresentation> candidates = new HashMap<>();
		Set<String> positionIds = positionsById.fieldSet();
		for (String positionId : positionIds) {
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = positionsById.getInnerJson(positionIdField);
			boolean closed = isClosed(position, resume, companies, now);
			if(closed) {
				continue;
			}
			candidates.put(positionId, position);
		}
		Set<String> unwantedPositions = getPositionsOfRecruitersWhoDoNotWantTheResume(resume, candidates);
		CcpJsonRepresentation openPositions = CcpOtherConstants.EMPTY_JSON;
		for (Map.Entry<String, CcpJsonRepresentation> candidate : candidates.entrySet()) {
			String positionId = candidate.getKey();
			boolean unwanted = unwantedPositions.contains(positionId);
			if(unwanted) {
				continue;
			}
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = candidate.getValue();
			openPositions = openPositions.put(positionIdField, position);
		}
		CcpJsonRepresentation response = json.put(Fields.positions, openPositions);
		return response;
	}

	/**
	 * Whether the support confirmed that the LinkedIn address of the resume does not open; the flag may come as a text.
	 * @param resume the resume, as stored
	 * @return {@code true} when the resume goes to no recruiter
	 */
	public static boolean hasTheLinkedinConfirmedInvalid(CcpJsonRepresentation resume) {
		boolean invalidLinkedin = resume.containsAllFields(VisEntityResume.Fields.invalidLinkedinAddress) && resume.getAsBoolean(VisEntityResume.Fields.invalidLinkedinAddress);
		return invalidLinkedin;
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

	// a position read by id may be gone, of another frequency, expired, asking what the resume does not have or of a
	// company the resume forbids
	private static boolean isClosed(CcpJsonRepresentation position, CcpJsonRepresentation resume, List<String> companies, long now) {
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
		boolean forbiddenCompany = isOfAForbiddenCompany(position, companies);
		if (forbiddenCompany) {
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

	// the company of the recruiter who owns the position or of the e-mail that receives its resumes
	private static boolean isOfAForbiddenCompany(CcpJsonRepresentation position, List<String> companies) {
		boolean nothingForbidden = companies.isEmpty();
		if (nothingForbidden) {
			return false;
		}
		String recruiterDomain = position.getOrDefault(VisEntityPosition.Fields.recruiterDomain, () -> "");
		boolean forbidsTheRecruiter = false == recruiterDomain.isEmpty() && VisBusinessSendResumesToRecruiters.isForbiddenCompany(recruiterDomain, companies);
		if (forbidsTheRecruiter) {
			return true;
		}
		String resumesEmail = position.getOrDefault(VisEntityPosition.Fields.resumesEmail, () -> "");
		boolean forbidsTheRecipient = false == resumesEmail.isEmpty() && VisBusinessSendResumesToRecruiters.isTheCompanyOfTheRecruiterForbidden(resumesEmail, companies);
		return forbidsTheRecipient;
	}

	// the positions whose recruiter, or whose e-mail that receives the resumes, keeps the view of this resume in the twin:
	// the ids of the views (the e-mails and the LinkedIn already hashed) read in one go
	private static Set<String> getPositionsOfRecruitersWhoDoNotWantTheResume(CcpJsonRepresentation resume, Map<String, CcpJsonRepresentation> positions) {
		boolean noPosition = positions.isEmpty();
		if (noPosition) {
			return Set.of();
		}
		String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
		String linkedinHash = VisBusinessSendResumesToRecruiters.getLinkedinHash(linkedinAddress);
		CcpEntity viewsByHashes = CcpEntityFactory.getCustomEntity(VisEntityViewedResume.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		Map<String, List<String>> viewIdsByPosition = new HashMap<>();
		List<String> viewIds = new ArrayList<>();
		for (Map.Entry<String, CcpJsonRepresentation> entry : positions.entrySet()) {
			CcpJsonRepresentation position = entry.getValue();
			List<String> recruiterHashes = getRecruiterHashes(position);
			List<String> viewIdsOfThePosition = new ArrayList<>();
			for (String recruiterHash : recruiterHashes) {
				CcpJsonRepresentation viewKey = CcpOtherConstants.EMPTY_JSON
						.put(VisEntityViewedResume.Fields.linkedin, linkedinHash)
						.put(VisEntityViewedResume.Fields.email, recruiterHash);
				String viewId = viewsByHashes.calculateId(viewKey);
				viewIdsOfThePosition.add(viewId);
			}
			viewIds.addAll(viewIdsOfThePosition);
			String positionId = entry.getKey();
			viewIdsByPosition.put(positionId, viewIdsOfThePosition);
		}
		CcpEntity inactiveViews = VisEntityViewedResume.ENTITY.getTwinEntity();
		CcpEntityMetaData inactiveViewsDetails = inactiveViews.getEntityMetaData();
		CcpJsonRepresentation inactiveViewsById = inactiveViewsDetails.getMultipleByRecordIds(viewIds);
		// the read by ids answers every id asked, empty when the record does not exist
		Set<String> foundViewIds = new HashSet<>();
		for (String viewId : viewIds) {
			CcpFieldName viewIdField = new CcpFieldName(viewId);
			CcpJsonRepresentation inactiveView = inactiveViewsById.getInnerJson(viewIdField);
			boolean notFound = inactiveView.isEmpty();
			if (notFound) {
				continue;
			}
			foundViewIds.add(viewId);
		}
		Set<String> unwantedPositions = new HashSet<>();
		for (Map.Entry<String, List<String>> entry : viewIdsByPosition.entrySet()) {
			List<String> viewIdsOfThePosition = entry.getValue();
			boolean unwanted = viewIdsOfThePosition.stream().anyMatch(foundViewIds::contains);
			if (false == unwanted) {
				continue;
			}
			String positionId = entry.getKey();
			unwantedPositions.add(positionId);
		}
		return unwantedPositions;
	}

	// the recruiter who owns the position (kept as the SHA-1 of the e-mail) and the e-mail that receives its resumes
	private static List<String> getRecruiterHashes(CcpJsonRepresentation position) {
		List<String> recruiterHashes = new ArrayList<>();
		String recruiterHash = position.getAsString(VisEntityPosition.Fields.recruiter);
		recruiterHashes.add(recruiterHash);
		String resumesEmail = position.getOrDefault(VisEntityPosition.Fields.resumesEmail, () -> "");
		boolean noRecipient = resumesEmail.isEmpty();
		if (noRecipient) {
			return recruiterHashes;
		}
		String recipientHash = VisBusinessSendResumesToRecruiters.getEmailHash(resumesEmail);
		recruiterHashes.add(recipientHash);
		return recruiterHashes;
	}
}
