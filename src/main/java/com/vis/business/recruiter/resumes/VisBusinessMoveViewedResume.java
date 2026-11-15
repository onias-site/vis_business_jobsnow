package com.vis.business.recruiter.resumes;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpBusinessDeleteRecordFromEntity;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityViewedResume;

/**
 * "Negativar CV" and "Positivar CV" of the screen of the viewed resumes (since 2026-11-15): the recruiter logged in moves
 * a view of a month between the sides of {@code vis_viewed_resume} ({@link VisViewedResumeMoves}). In the twin
 * ({@code vis_inactive_viewed_resume}) the resume no longer goes to the recruiter in the e-mail of resumes. The view keeps
 * its alias, comment and date, and its child goes with it between the sides of the grouper.
 *
 * <p>The view is found by the hash of its LinkedIn in the group of the month, on both sides; the presence on each side is
 * one read (FSED). A view already on the side reached is left as it is.
 *
 * <p>Input: {@code email}, {@code month} and {@code linkedinHash}. Output: {@code linkedinHash}.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when no view of the recruiter in the month has a LinkedIn of this hash
 */
public class VisBusinessMoveViewedResume implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The recruiter. */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		email,

		/** The month of the view ({@code yyyyMM}), or {@code all} for the group of every view of the recruiter. */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		month,

		/** The hash of the LinkedIn of the viewed resume, as the list of the views shows it. */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumeDetails.Fields.class)
		linkedinHash,
	}

	/** The fields of the procedure of the move. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The move made. */
	private final VisViewedResumeMoves move;

	/**
	 * Keeps the move.
	 * @param move the move made
	 */
	public VisBusinessMoveViewedResume(VisViewedResumeMoves move) {
		this.move = move;
	}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation storedView = VisBusinessViewedResumesOfTheMonth.getViewOfTheMonth(json);
		String linkedin = storedView.getAsString(VisEntityViewedResume.Fields.linkedin);
		String email = json.getAsString(Fields.email);
		CcpJsonRepresentation viewKey = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityViewedResume.Fields.linkedin, linkedin)
				.put(VisEntityViewedResume.Fields.email, email);
		CcpEntity sideLeft = this.move.getSideLeft();
		CcpEntity sideReached = this.move.getSideReached();
		CcpBusiness moveTheView = new CcpBusinessDeleteRecordFromEntity(sideLeft, viewKey);
		CcpBusiness refuseWhenNotFound = new VisBusinessRefuseViewedResumeNotFound(json);

		new CcpGetEntityId(viewKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(sideLeft).executeAction(moveTheView).and()
			.ifThisIdIsPresentInEntity(sideReached).executeAction(CcpOtherConstants.DO_NOTHING).and()
			.executeAction(refuseWhenNotFound)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = json.getJsonPiece(Fields.linkedinHash);
		return response;
	}
}
