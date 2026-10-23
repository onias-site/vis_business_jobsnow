package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.business.messages.JnBusinessCancelSupportPendingCommand;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.messages.VisMessages;

/**
 * Action of {@code VisServiceSkillFixHierarchy.DeleteSkillFixHierarchy} when the request is pending: deletes the stored
 * request (with the readable email of the key in place of the stored hash) and takes the ticket of the operator out of
 * {@code /pendingTickets}.
 */
public class VisBusinessDeleteStoredFixHierarchyRequest implements CcpBusiness {

	/** The key of the request, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the key of the request.
	 * @param json the key of the request, as it came
	 */
	public VisBusinessDeleteStoredFixHierarchyRequest(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Deletes the pending request and cancels the ticket of the operator.
	 * @param jsonWithTheStoredRequest the json of the procedure, holding the request under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredRequest) {
		CcpJsonRepresentation storedRequest = jsonWithTheStoredRequest.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillFixHierarchyPending.ENTITY);
		CcpJsonRepresentation completeRequest = storedRequest.mergeWithAnotherJson(this.json);
		VisEntitySkillFixHierarchyPending.ENTITY.delete(completeRequest);
		// the operator's ticket of this request leaves /pendingTickets
		String noticeTemplateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.class.getName();
		JnBusinessCancelSupportPendingCommand.INSTANCE.cancel(noticeTemplateId, completeRequest);
		return jsonWithTheStoredRequest;
	}
}
