package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;

/**
 * The check of the title typed in the form of the position (since 2026-11-24), one read by id: answers 200 when the
 * logged-in user already has a position (not deleted) with this title created in the month informed, which the form
 * explains and does not let be saved, and 404 when the title is free. With no month, the current one (the month of a
 * position being created).
 *
 * @throws CcpErrorFlowDisturb {@code 404} when the title is free
 */
public class VisBusinessPositionTitleInUse implements CcpBusiness {

	/** The input of the business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The logged-in user, the recruiter. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		/** The title typed in the form. */
		@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
		@CcpJsonFieldValidatorRequired
		title,

		/** The month of the creation of the position being edited; absent in the creation. */
		@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
		yearMonth,
		;
	}

	/** The fields of the procedure. */
	enum ProcedureFields implements CcpJsonFieldName {
		/** No field: the procedure returns nothing. */
		inexistentField,
	}

	/** The single instance. */
	public static final VisBusinessPositionTitleInUse INSTANCE = new VisBusinessPositionTitleInUse();

	private VisBusinessPositionTitleInUse() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String currentYearMonth = VisPositionOfTheRecruiter.getCurrentYearMonth();
		String yearMonth = json.getOrDefault(Fields.yearMonth, () -> currentYearMonth);
		CcpJsonRepresentation jsonWithYearMonth = json.put(Fields.yearMonth, yearMonth);
		CcpJsonRepresentation key = VisPositionOfTheRecruiter.getKey(jsonWithYearMonth);

		new CcpGetEntityId(key)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntityPosition.ENTITY).returnStatus(CcpProcessStatusDefault.NOT_FOUND).and()
			.executeAction(CcpOtherConstants.DO_NOTHING)
			.andFinallyReturningTheseFields(ProcedureFields.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		CcpJsonRepresentation response = jsonWithYearMonth.getJsonPiece(Fields.title, Fields.yearMonth);
		return response;
	}
}
