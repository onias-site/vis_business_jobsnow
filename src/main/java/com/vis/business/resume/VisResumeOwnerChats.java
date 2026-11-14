package com.vis.business.resume;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.jn.entities.JnEntityInstantMessengerUser;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.json.fields.validation.VisResumeMessageTypes;

/**
 * The instant messages to the owners of resumes (since 2026-11-13): the chat of each owner comes from
 * {@code jn_instant_messenger_user}, found by the e-mail of the resume, and no longer from the resume itself. An owner
 * without a record there gets nothing.
 */
public final class VisResumeOwnerChats {

	/** Utility class; not instantiable. */
	private VisResumeOwnerChats() {}

	/** The procedure answers nothing. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field: the procedure returns no field. */
		inexistentField
	}

	/**
	 * Saves the message in {@code vis_resume_message} for each e-mail that has a chat, every chat read by id in a single
	 * union all.
	 * @param origin who sends the message
	 * @param hashedEmails the e-mails of the owners, already hashed as the resumes keep them
	 * @param messageType the kind of the message
	 * @param message the text of the message, already in the language of the owner
	 */
	public static void saveTheMessage(CcpJsonFieldName origin, List<String> hashedEmails, VisResumeMessageTypes messageType, String message) {
		boolean noOwner = hashedEmails.isEmpty();
		if(noOwner) {
			return;
		}
		// the e-mails are already hashed: without the fields transformer, the id is calculated from them as they are,
		// instead of from a second hash
		CcpEntity instantMessengerUsers = CcpEntityFactory.getCustomEntity(JnEntityInstantMessengerUser.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		int ownersCount = hashedEmails.size();
		CcpJsonRepresentation[] ownerKeys = new CcpJsonRepresentation[ownersCount];
		for (int index = 0; index < ownersCount; index++) {
			String hashedEmail = hashedEmails.get(index);
			ownerKeys[index] = CcpOtherConstants.EMPTY_JSON.put(JnEntityInstantMessengerUser.Fields.email, hashedEmail);
		}
		CcpBusiness saveTheMessage = new VisBusinessSaveTheMessageToTheOwner(instantMessengerUsers, messageType, message);

		new CcpGetEntityId(ownerKeys)
		.toBeginProcedureAnd()
			.forEachIdPresentInEntity(instantMessengerUsers).executeAction(saveTheMessage)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(origin, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
	}
}
