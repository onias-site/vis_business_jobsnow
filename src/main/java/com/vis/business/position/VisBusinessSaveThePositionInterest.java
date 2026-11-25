package com.vis.business.position;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualPositionInterestHour;
import com.vis.status.VisProcessStatusPositionInterest;

/**
 * Step of {@link VisBusinessRegisterInterestInThePosition}, run when the resume of the candidate is active, has no
 * restriction and was neither sent nor promised to the position: checks both sides once more, now with the stored
 * resume and position, and saves the interest with the next full hour as the hour of its sending.
 * <ul>
 * <li>the LinkedIn address of the resume confirmed invalid: 422;</li>
 * <li>the position missing or inactive: 404; expired: 410;</li>
 * <li>the position not open to the resume ({@link VisBusinessPositionsOpenToTheResume}): 404.</li>
 * </ul>
 */
public class VisBusinessSaveThePositionInterest implements CcpBusiness {

	/** The id of the position. */
	private final String positionId;

	/** The e-mail of the candidate logged in, as typed. */
	private final String email;

	/**
	 * Keeps the position and the candidate.
	 * @param positionId the id of the position
	 * @param email the e-mail of the candidate logged in
	 */
	public VisBusinessSaveThePositionInterest(String positionId, String email) {
		this.positionId = positionId;
		this.email = email;
	}

	/**
	 * Saves the interest.
	 * @param json the json of the procedure, holding the stored resume under {@code _entities}
	 * @return the json with the hour of the sending
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation resume = json.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntityResume.ENTITY);
		boolean invalidLinkedin = VisBusinessPositionsOpenToTheResume.hasTheLinkedinConfirmedInvalid(resume);
		if(invalidLinkedin) {
			return VisProcessStatusPositionInterest.resumeWithPendingIssues.throwException(json);
		}
		CcpEntityMetaData positionDetails = VisEntityPosition.ENTITY.getEntityMetaData();
		List<String> positionIds = List.of(this.positionId);
		CcpJsonRepresentation positionsById = positionDetails.getMultipleByRecordIds(positionIds);
		CcpFieldName positionIdField = new CcpFieldName(this.positionId);
		CcpJsonRepresentation position = positionsById.getInnerJson(positionIdField);
		boolean notFound = position.isEmpty();
		if(notFound) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
		long now = System.currentTimeMillis();
		boolean expired = VisBusinessPositionsOpenToTheResume.isExpired(position, now);
		if(expired) {
			return VisProcessStatusPositionInterest.expiredPosition.throwException(json);
		}
		CcpJsonRepresentation openInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessPositionsOpenToTheResume.Fields.resume, resume)
				.put(VisBusinessPositionsOpenToTheResume.Fields.restricted, false)
				.put(VisBusinessPositionsOpenToTheResume.Fields.positions, positionsById);
		CcpJsonRepresentation openOutput = VisBusinessPositionsOpenToTheResume.INSTANCE.execute(openInput);
		CcpJsonRepresentation openPositions = openOutput.getInnerJson(VisBusinessPositionsOpenToTheResume.Fields.positions);
		boolean closed = false == openPositions.containsAllFields(positionIdField);
		if(closed) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
		String resumeId = VisEntityResume.ENTITY.calculateId(resume);
		String sendingHour = VisEntityVirtualPositionInterestHour.getNextSendingHour(now);
		CcpJsonRepresentation interest = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPositionInterest.Fields.position, this.positionId)
				.put(VisEntityPositionInterest.Fields.email, this.email)
				.put(VisEntityPositionInterest.Fields.resumeId, resumeId)
				.put(VisEntityPositionInterest.Fields.sendingHour, sendingHour);
		VisEntityPositionInterest.ENTITY.save(interest);
		CcpJsonRepresentation response = json.put(VisEntityPositionInterest.Fields.sendingHour, sendingHour);
		return response;
	}
}
