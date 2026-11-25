package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.entities.JnEntityEmailSuppressed;
import com.jn.entities.JnEntityGrouper;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionInterest;
import com.vis.entities.VisEntityVirtualPositionCandidates;
import com.vis.status.VisProcessStatusSavePosition;

/**
 * "Editar" of the screen "Meu Recrutamento - Vagas": saves again a position of the logged-in user with what they
 * changed. The key is the recruiter, the title and the month of the creation ({@code yearMonth}, which the edition never
 * changes). Since 2026-11-24 the title can change: with {@code oldTitle} different from {@code title} the position of the
 * old title disappears without a trace and the one of the new title takes its place, carrying the resumes already sent;
 * a position with candidates ({@code byTheCandidates}) keeps its title. The save is asynchronous.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the user has no position with this title (the old one, when renaming);
 * {@code 409} when the new title is of another position of the same month; {@code 410} when the e-mail of the resumes is
 * suppressed; {@code 423} when a position with candidates would change its title
 */
public class VisBusinessEditPosition implements CcpBusiness {

	/** The fields of the procedure of the save. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The single instance. */
	public static final VisBusinessEditPosition INSTANCE = new VisBusinessEditPosition();

	private VisBusinessEditPosition() {}

	public Class<?> getJsonValidationClass() {
		return VisPositionOfTheRecruiterFields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the month of the creation is part of the key of the stored position: the edition cannot do without it
		boolean noYearMonth = false == json.containsAllFields(VisPositionOfTheRecruiterFields.yearMonth);
		if(noYearMonth) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.UNPROCESSABLE_ENTITY);
		}
		CcpJsonRepresentation position = VisPositionOfTheRecruiter.fromInput(json);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(json);
		// the e-mail that receives the resumes, as the key of the suppressions; absent (frequency none), the step is skipped
		CcpJsonRepresentation resumesEmail = json.getJsonPiece(VisPositionOfTheRecruiterFields.resumesEmail);
		CcpJsonRepresentation suppressionKey = resumesEmail.renameField(VisPositionOfTheRecruiterFields.resumesEmail, JnEntityEmailSuppressed.Fields.email);
		String title = json.getAsString(VisPositionOfTheRecruiterFields.title);
		String oldTitle = json.getOrDefault(VisPositionOfTheRecruiterFields.oldTitle, () -> title);
		boolean renaming = false == oldTitle.equals(title);
		if(renaming) {
			return this.rename(json, position, key, suppressionKey, oldTitle);
		}
		CcpBusiness save = new VisBusinessSavePosition(position);

		new CcpGetEntityId(key, suppressionKey)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(JnEntityEmailSuppressed.ENTITY).returnStatus(VisProcessStatusSavePosition.suppressedEmail).and()
			.executeAction(save)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = position.removeFields(VisEntityPosition.Fields.recruiter);
		return response;
	}

	/**
	 * The edition that changes the title: the position of the old title must exist (404), have no candidates (423) and the
	 * e-mail of the resumes must not be suppressed (410), all in one read; then {@link VisBusinessRenamePosition} checks
	 * the new title (409) and replaces the position.
	 */
	private CcpJsonRepresentation rename(CcpJsonRepresentation json, CcpJsonRepresentation position, CcpJsonRepresentation newKey,
			CcpJsonRepresentation suppressionKey, String oldTitle) {
		CcpJsonRepresentation oldKey = VisPositionOfTheRecruiter.getKey(json, oldTitle);
		CcpJsonRepresentation hashedOldKey = VisEntityPosition.ENTITY.getJsonWithHandledPrimaryKey(oldKey);
		String oldPositionId = VisEntityPosition.ENTITY.calculateId(hashedOldKey);
		String candidatesParentId = VisEntityVirtualPositionCandidates.getParentId(oldPositionId);
		CcpEntityMetaData candidatesDetails = VisEntityVirtualPositionCandidates.ENTITY.getEntityMetaData();
		CcpEntityMetaData interestDetails = VisEntityPositionInterest.ENTITY.getEntityMetaData();
		CcpJsonRepresentation candidatesGroupKey = JnGroupers.getGroupKey(candidatesDetails.entityName, interestDetails.entityName, candidatesParentId);
		CcpBusiness rename = new VisBusinessRenamePosition(position, newKey, oldKey);

		new CcpGetEntityId(oldKey, suppressionKey, candidatesGroupKey)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.ifThisIdIsPresentInEntity(JnEntityGrouper.ENTITY).returnStatus(VisProcessStatusSavePosition.positionWithCandidates).and()
			.ifThisIdIsPresentInEntity(JnEntityEmailSuppressed.ENTITY).returnStatus(VisProcessStatusSavePosition.suppressedEmail).and()
			.executeAction(rename)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = position.removeFields(VisEntityPosition.Fields.recruiter);
		return response;
	}
}
