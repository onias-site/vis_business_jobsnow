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
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityPositionResumeSent;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualHashGrouper;

/**
 * The positions of the resume of the candidate logged in (screen "Meus dados → Minhas vagas", since 2026-11-24): the
 * positions open to the resume ({@link VisBusinessPositionsOpenToTheResume}: open to the candidates themselves, still
 * valid, satisfied by the resume, of no company it forbids and of no recruiter who does not want it). Online and with
 * no query (OQL): the resume keeps the ids of its groups of hashes, the positions are the children of the same groups on
 * the side of the positions, read by id. Only the fields the candidate may see go back, plus the id of the position (to
 * send the resume to it) and whether the resume was already sent to it ({@code resumeSent}) or will be in the next full
 * hour ({@code sendingHour}); never the recruiter, the e-mail that receives the resumes nor the internal fields. A
 * resume with the LinkedIn address confirmed invalid or with a restriction to correct sees no position
 * ({@code resumeWithPendingIssues}). A candidate without a resume gets 404, one with the resume inactive 301.
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
		/** Whether the resume has something to correct before seeing the positions (invalid LinkedIn, restriction). */
		resumeWithPendingIssues,
		/** Whether the resume was already sent to the position. */
		resumeSent,
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
		CcpEntity inactiveResume = VisEntityResume.ENTITY.getTwinEntity();
		// the resume and its restriction in one read; an inactive resume answers 301, a missing one 404
		CcpJsonRepresentation resultingData = new CcpGetEntityId(resumeKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(inactiveResume).returnStatus(CcpProcessStatusDefault.REDIRECT).and()
			.ifThisIdIsNotPresentInEntity(VisEntityResume.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.loadThisIdFromEntity(VisEntityResume.ENTITY)
			.andFinally(CcpEntity.JsonFieldNames._entities)
		.endThisProcedureRetrievingTheResultingData(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation storedRecords = resultingData.getInnerJson(CcpEntity.JsonFieldNames._entities);
		CcpJsonRepresentation resume = storedRecords.getInnerJson(VisEntityResume.ENTITY);
		CcpJsonRepresentation positionsById = getPositionsOfTheGroupsOfTheResume(resume);
		CcpJsonRepresentation openInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessPositionsOpenToTheResume.Fields.resume, resume)
				.put(VisBusinessPositionsOpenToTheResume.Fields.positions, positionsById);
		CcpJsonRepresentation openOutput = VisBusinessPositionsOpenToTheResume.INSTANCE.execute(openInput);
		// the rules of the resume alone (the restriction, the LinkedIn confirmed invalid) leave no position open
		boolean pendingIssues = openOutput.getAsBoolean(VisBusinessPositionsOpenToTheResume.Fields.blocked);
		if(pendingIssues) {
			return CcpOtherConstants.EMPTY_JSON
					.put(ResponseFields.positions, List.of())
					.put(ResponseFields.resumeWithPendingIssues, true);
		}
		CcpJsonRepresentation openPositions = openOutput.getInnerJson(VisBusinessPositionsOpenToTheResume.Fields.positions);
		List<CcpJsonRepresentation> visiblePositions = getVisiblePositions(openPositions, email, resume);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON
				.put(ResponseFields.positions, visiblePositions)
				.put(ResponseFields.resumeWithPendingIssues, false);
		return response;
	}

	// the children of the groups of the hashes of the resume, on the side of the positions, read by id
	private static CcpJsonRepresentation getPositionsOfTheGroupsOfTheResume(CcpJsonRepresentation resume) {
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
		return positionsById;
	}

	// the fields the candidate sees, the id and where the sending of the resume to each position stands (one read of
	// the interests and of the resumes already sent, by id)
	private static List<CcpJsonRepresentation> getVisiblePositions(CcpJsonRepresentation openPositions, String email, CcpJsonRepresentation resume) {
		Set<String> positionIds = openPositions.fieldSet();
		boolean noPosition = positionIds.isEmpty();
		if(noPosition) {
			return List.of();
		}
		String emailHash = resume.getAsString(VisEntityResume.Fields.email);
		List<CcpJsonRepresentation> keys = new ArrayList<>();
		for (String positionId : positionIds) {
			CcpJsonRepresentation key = getSendingKey(positionId, email, emailHash);
			keys.add(key);
		}
		int keysCount = keys.size();
		CcpJsonRepresentation[] keysArray = keys.toArray(new CcpJsonRepresentation[keysCount]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll sendings = crud.unionAll(keysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityPositionInterest.ENTITY, VisEntityPositionResumeSent.ENTITY);
		List<CcpJsonRepresentation> visiblePositions = new ArrayList<>();
		for (CcpJsonRepresentation key : keys) {
			String positionId = key.getAsString(VisEntityPositionInterest.Fields.position);
			CcpFieldName positionIdField = new CcpFieldName(positionId);
			CcpJsonRepresentation position = openPositions.getInnerJson(positionIdField);
			boolean resumeSent = VisEntityPositionResumeSent.ENTITY.isPresentInThisUnionAll(sendings, key);
			CcpJsonRepresentation visiblePosition = position.getJsonPiece(VISIBLE_FIELDS)
					.put(JnJsonCommonsFields.id, positionId)
					.put(ResponseFields.resumeSent, resumeSent);
			boolean interested = VisEntityPositionInterest.ENTITY.isPresentInThisUnionAll(sendings, key);
			if(false == interested) {
				visiblePositions.add(visiblePosition);
				continue;
			}
			CcpJsonRepresentation interest = VisEntityPositionInterest.ENTITY.getRecordFromUnionAll(sendings, () -> key);
			String sendingHour = interest.getAsString(VisEntityPositionInterest.Fields.sendingHour);
			CcpJsonRepresentation positionWithTheHour = visiblePosition.put(VisEntityPositionInterest.Fields.sendingHour, sendingHour);
			visiblePositions.add(positionWithTheHour);
		}
		return visiblePositions;
	}

	/**
	 * The key of the interest of the candidate in the position and of the resume already sent to it, the same json for
	 * both entities.
	 * @param positionId the id of the position
	 * @param email the e-mail of the candidate, as typed
	 * @param emailHash the e-mail of the candidate, hashed as {@code vis_resume} keeps it
	 * @return the key
	 */
	public static CcpJsonRepresentation getSendingKey(String positionId, String email, String emailHash) {
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPositionInterest.Fields.position, positionId)
				.put(VisEntityPositionInterest.Fields.email, email)
				.put(VisEntityPositionResumeSent.Fields.resume, emailHash);
		return key;
	}
}
