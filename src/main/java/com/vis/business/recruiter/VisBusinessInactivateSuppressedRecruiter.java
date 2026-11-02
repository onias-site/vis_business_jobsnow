package com.vis.business.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpBusinessDeleteStoredRecordFromEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityRecruiter;

/**
 * Subscriber of the first write of {@code jn_email_suppressed} (through {@code jn_write_event_subscribers}): a recruiter
 * whose address became suppressed goes to the inactive recruiters, because no e-mail reaches it anymore. Runs in the
 * consumer of the messaging. An address that is not of a recruiter changes nothing, and the sign-up of the recruiter brings
 * it back ({@link VisBusinessSaveRecruiterFromLoginAnswers}).
 * <p>Input: the suppression as written ({@code email} as a hash with the readable one in {@code originalEmail}).
 */
public class VisBusinessInactivateSuppressedRecruiter implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessInactivateSuppressedRecruiter INSTANCE = new VisBusinessInactivateSuppressedRecruiter();

	private VisBusinessInactivateSuppressedRecruiter() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the suppression
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		String loginSource = VisEntityRecruiter.Sources.login.getValue();
		CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
		// the source only completes the record: the delete uses the stored one
		CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, loginSource);
		CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);
		// deleting from the recruiters moves the given record to the twin
		CcpBusiness inactivate = new CcpBusinessDeleteStoredRecordFromEntity(VisEntityRecruiter.ENTITY);

		new CcpGetEntityId(recruiter)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntityRecruiter.ENTITY).executeAction(inactivate)
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
