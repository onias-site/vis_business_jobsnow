package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.entities.JnEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillPending;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnUserRequestCommands;

/**
 * Action of {@code VisBusinessSkillSuggestionIgnoreUser}: records the user in {@link JnEntityCommandNotAllowedToUser},
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
		CcpJsonRepresentation descriptionWithEmail = requestDescription.put(VisEntitySkillPending.Fields.email, email);
		// the summary of the request shown in the lists of ignored users (jn): the skill
		String skill = this.suggestionKey.getAsString(VisEntitySkillPending.Fields.skill);
		CcpJsonRepresentation description = descriptionWithEmail.put(JnJsonCommonsFields.request, skill);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(JnEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithCommand = ignoredUserWithEmail.put(JnEntityCommandNotAllowedToUser.Fields.commandName, JnUserRequestCommands.reviewSkillSuggestion);
		CcpJsonRepresentation ignoredUser = ignoredUserWithCommand.put(JnEntityCommandNotAllowedToUser.Fields.description, description);
		// saving the user discards this suggestion and every other pending request of the user
		JnEntityCommandNotAllowedToUser.ENTITY.save(ignoredUser);
		return jsonWithTheStoredSuggestion;
	}
}
