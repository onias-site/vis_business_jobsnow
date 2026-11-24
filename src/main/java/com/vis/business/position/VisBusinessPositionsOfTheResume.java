package com.vis.business.position;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.json.fields.validation.VisPositionFrequencyTypes;
import com.vis.json.transformers.VisPositionPublishingDayCombinations;

/**
 * The positions of the resume of the candidate logged in (screen "Meus dados → Minhas vagas", since 2026-11-24): the
 * positions open to the candidates themselves ({@code frequencyType} {@code byTheCandidates}), still valid, that the
 * resume satisfies. Online and with no query (OQL): the resume keeps the ids of its groups of hashes, the positions are
 * the children of the same groups on the side of the positions, read by id, and the ranges, regions, flags and forbidden
 * companies are compared here, with the rules of the e-mails of the positions ({@link VisBusinessResumeMatchesTheFilters}).
 * Only the fields the candidate may see go back: never the recruiter, the e-mail that receives the resumes nor the
 * internal fields. A candidate without an active resume gets the error of the resume not found (404).
 */
public class VisBusinessPositionsOfTheResume implements CcpBusiness {

	/** The input of the business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The candidate logged in. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,
	}

	/** The output of the business. */
	public static enum ResponseFields implements CcpJsonFieldName {
		/** The positions, with only the fields shown to the candidate. */
		positions,
	}

	/** The fields of the position the candidate sees. */
	private static final CcpJsonFieldName[] VISIBLE_FIELDS = {
			VisEntityPosition.Fields.title,
			VisEntityPosition.Fields.description,
			VisEntityPosition.Fields.resumeType,
			VisEntityPosition.Fields.seniority,
			VisEntityPosition.Fields.ddd,
			VisEntityPosition.Fields.clt,
			VisEntityPosition.Fields.pj,
			VisEntityPosition.Fields.expiration,
			VisEntityPosition.Fields.contact,
			VisEntityPosition.Fields.disponibility,
			VisEntityPosition.Fields.temporallyJobTime,
			VisEntityPosition.Fields.spokenLanguage,
			VisEntityPosition.Fields.skill,
			VisEntityPosition.Fields.pcd,
			VisEntityPosition.Fields.travel,
	};

	/** The single instance: the class keeps no state. */
	public static final VisBusinessPositionsOfTheResume INSTANCE = new VisBusinessPositionsOfTheResume();

	private VisBusinessPositionsOfTheResume() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(Fields.email);
		CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, email);
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getOneById(resumeKey);
		List<String> hashes = resume.getAsStringList(VisEntityResume.Fields.hash);
		String parentName = VisEntityVirtualHashGrouper.ENTITY.getEntityMetaData().entityName;
		String childName = VisEntityPosition.ENTITY.getEntityMetaData().entityName;
		Map<String, List<String>> positionsByHash = JnGroupers.getChildrenOfGroups(parentName, childName, hashes);
		Set<String> positionIds = new LinkedHashSet<>();
		for (List<String> positionsOfTheHash : positionsByHash.values()) {
			positionIds.addAll(positionsOfTheHash);
		}
		CcpEntityMetaData positionDetails = VisEntityPosition.ENTITY.getEntityMetaData();
		CcpJsonRepresentation positionsById = positionDetails.getMultipleByRecordIds(positionIds);
		long now = System.currentTimeMillis();
		List<CcpJsonRepresentation> visiblePositions = new ArrayList<>();
		for (String positionId : positionIds) {
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = positionsById.getInnerJson(positionIdField);
			boolean shown = isShownToTheResume(position, resume, now);
			if (false == shown) {
				continue;
			}
			CcpJsonRepresentation visiblePosition = position.getJsonPiece(VISIBLE_FIELDS);
			visiblePositions.add(visiblePosition);
		}
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(ResponseFields.positions, visiblePositions);
		return response;
	}

	// a position read from the group may be gone (inactivated or deleted after the group was read), of another frequency,
	// expired or, out of the hash, asking what the resume does not have
	private static boolean isShownToTheResume(CcpJsonRepresentation position, CcpJsonRepresentation resume, long now) {
		boolean notFound = position.isEmpty();
		if (notFound) {
			return false;
		}
		String frequencyType = position.getOrDefault(VisEntityPosition.Fields.frequencyType, () -> "");
		boolean byTheCandidates = VisPositionFrequencyTypes.byTheCandidates.name().equals(frequencyType);
		if (false == byTheCandidates) {
			return false;
		}
		boolean expired = isExpired(position, now);
		if (expired) {
			return false;
		}
		CcpJsonRepresentation filters = VisBusinessFiltersOfThePosition.INSTANCE.execute(position);
		CcpJsonRepresentation matchInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessResumeMatchesTheFilters.Fields.filters, filters)
				.put(VisBusinessResumeMatchesTheFilters.Fields.resume, resume);
		CcpJsonRepresentation matchOutput = VisBusinessResumeMatchesTheFilters.INSTANCE.execute(matchInput);
		boolean matches = matchOutput.getAsBoolean(VisBusinessResumeMatchesTheFilters.Fields.matches);
		return matches;
	}

	private static boolean isExpired(CcpJsonRepresentation position, long now) {
		CcpJsonRepresentation expiration = position.getInnerJson(VisEntityPosition.Fields.expiration);
		boolean noEnd = false == expiration.containsAllFields(VisPositionPublishingDayCombinations.ExpirationFields.max);
		if (noEnd) {
			return false;
		}
		Double end = expiration.getAsDoubleNumber(VisPositionPublishingDayCombinations.ExpirationFields.max);
		boolean expired = end.longValue() < now;
		return expired;
	}
}
