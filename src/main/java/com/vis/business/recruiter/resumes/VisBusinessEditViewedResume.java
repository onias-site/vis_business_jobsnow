package com.vis.business.recruiter.resumes;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityViewedResume;

/**
 * "Editar detalhes" of the screen of the viewed resumes (since 2026-11-15): the recruiter logged in changes the
 * {@code alias} and the {@code comment} of a view of a month, on the side of {@code vis_viewed_resume} where it is. A view
 * of the twin ({@code vis_inactive_viewed_resume}, tab "Indesejados") is saved in the twin and stays there.
 *
 * <p>The view is found by the hash of its LinkedIn in the group of the month, on both sides (the LinkedIn never leaves to
 * the screen), and keeps the {@code date} of the last view: the edit is not a view, so it stays in the same month and is
 * not counted again. The values of the resume the groups of the ddds keep come from the active resume that uses the
 * LinkedIn, as in a view. Every read is by id (OQL); the presence on each side is one read (FSED).
 *
 * <p>Input: {@code email}, {@code month}, {@code linkedinHash}, {@code alias} and {@code comment} (optional; the save
 * merges, so a comment already saved is kept when none comes). Output: {@code linkedinHash}, {@code alias} and
 * {@code comment}, as they came.
 *
 * @throws CcpErrorFlowDisturb {@code 404} when no view of the recruiter in the month has a LinkedIn of this hash
 */
public class VisBusinessEditViewedResume implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The recruiter. */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		email,

		/** The month of the view ({@code yyyyMM}). */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		month,

		/** The hash of the LinkedIn of the viewed resume, as the list of the views shows it. */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumeDetails.Fields.class)
		linkedinHash,

		/** The alias the recruiter gives to the resume. */
		@CcpJsonCopyFieldValidationsFrom(VisEntityViewedResume.Fields.class)
		@CcpJsonFieldValidatorRequired
		alias,

		/** The comment of the recruiter about the resume. */
		@CcpJsonCopyFieldValidationsFrom(VisEntityViewedResume.Fields.class)
		comment,
	}

	/** The fields of the procedure of the save. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The single instance. */
	public static final VisBusinessEditViewedResume INSTANCE = new VisBusinessEditViewedResume();

	private VisBusinessEditViewedResume() {}

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
		// the comment saved goes too: the record keeps it when none comes (the save merges), and so must the group
		CcpJsonRepresentation storedValues = storedView.getJsonPiece(VisEntityViewedResume.Fields.date, VisEntityViewedResume.Fields.ddd, VisEntityViewedResume.Fields.comment);
		CcpJsonRepresentation editedValues = json.getJsonPiece(Fields.alias, Fields.comment);
		CcpJsonRepresentation viewWithStoredValues = viewKey.mergeWithAnotherJson(storedValues);
		CcpJsonRepresentation editedView = viewWithStoredValues.mergeWithAnotherJson(editedValues);
		List<CcpJsonRepresentation> resumes = VisBusinessViewedResumeDetails.getActiveResumesOfTheLinkedin(linkedin, email);
		CcpJsonRepresentation viewToSave = putTheFirstResume(editedView, resumes);

		CcpEntity twinEntity = VisEntityViewedResume.ENTITY.getTwinEntity();
		CcpBusiness saveOnTheMainSide = new VisBusinessSaveViewedResumeOnSide(VisEntityViewedResume.ENTITY, viewToSave);
		CcpBusiness saveOnTheTwinSide = new VisBusinessSaveViewedResumeOnSide(twinEntity, viewToSave);
		CcpBusiness refuseWhenNotFound = new VisBusinessRefuseViewedResumeNotFound(json);

		new CcpGetEntityId(viewKey)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityViewedResume.ENTITY).executeAction(saveOnTheMainSide).and()
			.ifThisIdIsPresentInEntity(twinEntity).executeAction(saveOnTheTwinSide).and()
			.executeAction(refuseWhenNotFound)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = json.getJsonPiece(Fields.linkedinHash, Fields.alias, Fields.comment);
		return response;
	}

	/**
	 * The view with the values of the first active resume that uses its LinkedIn; the view as it is when none is active.
	 * @param view the view to be saved
	 * @param resumes the active resumes that use the LinkedIn
	 * @return the view to be saved
	 */
	private static CcpJsonRepresentation putTheFirstResume(CcpJsonRepresentation view, List<CcpJsonRepresentation> resumes) {
		boolean noActiveResume = resumes.isEmpty();

		if(noActiveResume) {
			return view;
		}

		CcpJsonRepresentation resume = resumes.get(0);
		CcpJsonRepresentation viewWithTheResume = putTheViewedResume(view, resume);
		return viewWithTheResume;
	}

	/**
	 * The view with the values of the viewed resume that the save keeps in the groups of its ddds: the ddds (stored in
	 * the view) and btc, clt, disponibility, experience, spokenLanguage, negotiableClaim, pcd, pj, resumeType,
	 * temporallyJobTime, travel and skill (only in the child of each group), as the resume is stored.
	 * @param view the view to be saved
	 * @param resume the viewed resume, as stored
	 * @return the view with those values
	 */
	public static CcpJsonRepresentation putTheViewedResume(CcpJsonRepresentation view, CcpJsonRepresentation resume) {
		CcpJsonRepresentation viewedResume = resume.getJsonPiece(VisEntityResume.Fields.ddd, VisEntityResume.Fields.btc, VisEntityResume.Fields.clt,
				VisEntityResume.Fields.disponibility, VisEntityResume.Fields.experience, VisEntityResume.Fields.spokenLanguage,
				VisEntityResume.Fields.negotiableClaim, VisEntityResume.Fields.pcd, VisEntityResume.Fields.pj, VisEntityResume.Fields.resumeType,
				VisEntityResume.Fields.temporallyJobTime, VisEntityResume.Fields.travel, VisEntityResume.Fields.skill);
		CcpJsonRepresentation viewWithTheResume = view.mergeWithAnotherJson(viewedResume);
		return viewWithTheResume;
	}
}
