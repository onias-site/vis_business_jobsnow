package com.vis.business.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpBusinessDeleteStoredRecordFromEntity;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityRecruiter;

/**
 * Runs after a save of {@code vis_viewed_resume}: viewing a resume makes an inactive recruiter (in
 * {@code vis_inactive_recruiter}) active again, with the record they had. A recruiter who is already active, or who is not
 * a recruiter, changes nothing.
 * <p>Input: the view as saved ({@code email} as a hash with the readable one in {@code originalEmail}).
 */
public class VisBusinessReactivateRecruiterWhoViewed implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessReactivateRecruiterWhoViewed INSTANCE = new VisBusinessReactivateRecruiterWhoViewed();

	private VisBusinessReactivateRecruiterWhoViewed() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the view
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean emailAlreadyHashed = json.containsField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonFieldName readableEmailField = emailAlreadyHashed ? JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail : VisEntityRecruiter.Fields.email;
		String email = json.getAsString(readableEmailField);
		String loginSource = VisEntityRecruiter.Sources.login.getValue();
		CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
		// the source only completes the record: the delete uses the stored one
		CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, loginSource);
		CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);

		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		// deleting from the twin moves the stored record back to the recruiters
		CcpBusiness reactivate = new CcpBusinessDeleteStoredRecordFromEntity(twinEntity);

		new CcpGetEntityId(recruiter)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(twinEntity).executeAction(reactivate)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}

	/** Fields of the procedure. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field: the procedure returns no field. */
		inexistentField
	}
}
