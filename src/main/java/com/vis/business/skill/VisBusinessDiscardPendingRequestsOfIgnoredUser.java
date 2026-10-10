package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryExecutorDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.business.messages.JnBusinessCancelSupportPendingCommand;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityCommandNotAllowedToUser;

/**
 * Runs after a user is saved in {@link VisEntityCommandNotAllowedToUser}, that is, when the support bot operator
 * decides to ignore the user: discards every request of the user still pending, of every kind
 * ({@link VisIgnoredUserPendingRequests}), as if it had never existed. Each request leaves the pending entity by
 * {@code deleteAnyWhere}, which also purges its versioned history, and its ticket leaves {@code /pendingTickets}.
 * Nothing is approved nor rejected and the user is not notified. The items of a skill hierarchy fix request that no
 * other user asks for go with it (the pending entity's own deletion rule).
 *
 * <p>Runs in the support bot, never in an online request, so querying the requests by email is allowed.
 */
public class VisBusinessDiscardPendingRequestsOfIgnoredUser implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the ignored user
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		// the pending entities keep the hash of the email, and deleting needs the readable one, since the entity
		// hashes it again to find the record
		boolean emailAlreadyHashed = json.containsField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonRepresentation jsonWithHashedEmail = emailAlreadyHashed ? json : json.getTransformedJson(JnJsonTransformersFieldsEntityDefault.email);
		String hashedEmail = jsonWithHashedEmail.getAsString(JnJsonCommonsFields.email);
		String readableEmail = jsonWithHashedEmail.getAsString(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);

		VisIgnoredUserPendingRequests[] kinds = VisIgnoredUserPendingRequests.values();

		for (VisIgnoredUserPendingRequests kind : kinds) {
			this.discardPendingRequests(kind, hashedEmail, readableEmail);
		}

		return json;
	}

	/**
	 * Discards the pending requests of one kind of the user.
	 * @param kind the kind of request
	 * @param hashedEmail the email as the pending entity keeps it
	 * @param readableEmail the email as the user typed it
	 */
	private void discardPendingRequests(VisIgnoredUserPendingRequests kind, String hashedEmail, String readableEmail) {

		CcpEntity pendingEntity = kind.getPendingEntity();
		CcpQueryOptions requestsOfTheUser = CcpQueryOptions.INSTANCE
				.startQuery()
				.startBool()
				.startMust()
				.term(JnJsonCommonsFields.email, hashedEmail)
				.endMustAndBackToBool()
				.endBoolAndBackToQuery()
				.endQueryAndBackToRequest()
				.maxResults();
		CcpQueryExecutorDecorator pendingRequestsQuery = requestsOfTheUser.selectFrom(pendingEntity);
		List<CcpJsonRepresentation> pendingRequests = pendingRequestsQuery.getResultAsList();

		Class<?> noticeTemplate = kind.getNoticeTemplate();
		String noticeTemplateId = noticeTemplate.getName();

		for (CcpJsonRepresentation pendingRequest : pendingRequests) {
			CcpJsonRepresentation completeRequest = pendingRequest.put(JnJsonCommonsFields.email, readableEmail);
			JnBusinessCancelSupportPendingCommand.INSTANCE.cancel(noticeTemplateId, completeRequest);
			pendingEntity.deleteAnyWhere(completeRequest);
		}
	}
}
