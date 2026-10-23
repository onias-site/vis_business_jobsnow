package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillPending;
import com.vis.json.fields.validation.VisUserRequestCommands;

/**
 * Action of {@code VisBusinessSkillSuggestionIgnoreUser}: records the user in {@link VisEntityCommandNotAllowedToUser},
 * with the suggestion being reviewed ({@code skill}, {@code synonym} and {@code description}) in the {@code description}.
 */
class VisBusinessSaveUserIgnoredInSkillSuggestion implements CcpBusiness {

	/** The suggestion key (email + skill), with the readable email. */
	private final CcpJsonRepresentation suggestionKey;

	/**
	 * Keeps the suggestion key.
	 * @param suggestionKey the suggestion key, with the readable email
	 */
	VisBusinessSaveUserIgnoredInSkillSuggestion(CcpJsonRepresentation suggestionKey) {
		this.suggestionKey = suggestionKey;
	}

	/**
	 * Records the ignored user.
	 * @param jsonWithTheStoredSuggestion the json of the procedure, holding the suggestion under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredSuggestion) {
		// empty when the suggestion is no longer pending
		CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
		CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(this.suggestionKey);
		CcpJsonRepresentation requestDescription = completeSuggestion.getJsonPiece(VisEntitySkillPending.Fields.skill, VisEntitySkillPending.Fields.synonym, VisEntitySkillPending.Fields.description);

		String email = this.suggestionKey.getAsString(VisEntitySkillPending.Fields.email);
		// the email of the record is kept as hash; the readable one goes in the description, for the operator to list the ignored users
		CcpJsonRepresentation description = requestDescription.put(VisEntitySkillPending.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithCommand = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, VisUserRequestCommands.reviewSkillSuggestion);
		CcpJsonRepresentation ignoredUser = ignoredUserWithCommand.put(VisEntityCommandNotAllowedToUser.Fields.description, description);
		// saving the user discards this suggestion and every other pending request of the user
		VisEntityCommandNotAllowedToUser.ENTITY.save(ignoredUser);
		return jsonWithTheStoredSuggestion;
	}
}
