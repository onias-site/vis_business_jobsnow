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
 * Subscriber of the save of the sign-up answers ({@code js_login_answers}, through {@code jn_write_event_subscribers}):
 * a user whose goal is {@code recruiting} becomes a recruiter. Runs in the consumer of the messaging, after the answers
 * are written.
 * <ul>
 * <li>already a recruiter: nothing changes (the record keeps its {@code source});</li>
 * <li>an inactive recruiter (in {@code vis_inactive_recruiter}): goes back to the recruiters, with the record it had;</li>
 * <li>neither: a new recruiter with the source {@code login}.</li>
 * </ul>
 * Both entities are read in a single union all. Another goal changes nothing.
 * <p>Input: the answers as written ({@code email} as a hash with the readable one in {@code originalEmail}, and
 * {@code goal}).
 */
public class VisBusinessSaveRecruiterFromLoginAnswers implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessSaveRecruiterFromLoginAnswers INSTANCE = new VisBusinessSaveRecruiterFromLoginAnswers();

	private VisBusinessSaveRecruiterFromLoginAnswers() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the answers
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String goal = json.getAsString(JsonFieldNames.goal);
		String recruitingGoal = JsonFieldNames.recruiting.getValue();
		boolean isNotRecruiting = false == recruitingGoal.equals(goal);

		if(isNotRecruiting) {
			return json;
		}

		// the transformer of the answers keeps the readable e-mail in originalEmail and the hash in email
		boolean emailAlreadyHashed = json.containsField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonFieldName readableEmailField = emailAlreadyHashed ? JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail : VisEntityRecruiter.Fields.email;
		String email = json.getAsString(readableEmailField);
		String loginSource = VisEntityRecruiter.Sources.login.getValue();
		CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
		CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, loginSource);
		CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);

		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		// deleting from the twin moves the given record back to the recruiters: the stored one, so the source is kept
		CcpBusiness reactivate = new CcpBusinessDeleteStoredRecordFromEntity(twinEntity);
		// a recruiter found in either entity is placed under _entities; with none, the recruiter is new
		CcpBusiness insertWhenNotFound = new VisBusinessInsertRecruiterWhenNotFound(recruiter);

		new CcpGetEntityId(recruiter)
		.toBeginProcedureAnd()
			.loadThisIdFromEntity(VisEntityRecruiter.ENTITY).and()
			.ifThisIdIsPresentInEntity(twinEntity).executeAction(reactivate).and()
			.executeAction(insertWhenNotFound)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}

	/** Fields of the answers and of the procedure. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code goal} field of the sign-up answers. */
		goal,
		/** The value of {@code goal} of a user who recruits. */
		recruiting,
		/** The {@code inexistentField} field: the procedure returns no field. */
		inexistentField
	}
}
