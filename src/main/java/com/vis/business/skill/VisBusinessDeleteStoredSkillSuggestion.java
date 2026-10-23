package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.business.messages.JnBusinessCancelSupportPendingCommand;
import com.vis.entities.VisEntitySkillPending;
import com.vis.messages.VisMessages;

/**
 * Action of {@code VisServiceSkillSuggestion.DeleteSkillSuggestion} when the suggestion is pending: deletes it and takes
 * the ticket of the operator out of {@code /pendingTickets}.
 */
public class VisBusinessDeleteStoredSkillSuggestion implements CcpBusiness {

	/** The key of the suggestion, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the key of the suggestion.
	 * @param json the key of the suggestion, as it came
	 */
	public VisBusinessDeleteStoredSkillSuggestion(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Deletes the pending suggestion and cancels the ticket of the operator.
	 * @param jsonWithTheStoredSuggestion the json of the procedure, holding the suggestion under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredSuggestion) {
		CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
		CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(this.json);
		VisEntitySkillPending.ENTITY.delete(completeSuggestion);
		// the operator's ticket of this suggestion leaves /pendingTickets
		String noticeTemplateId = VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class.getName();
		JnBusinessCancelSupportPendingCommand.INSTANCE.cancel(noticeTemplateId, completeSuggestion);
		return jsonWithTheStoredSuggestion;
	}
}
