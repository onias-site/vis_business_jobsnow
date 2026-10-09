package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillPending;
import com.vis.json.fields.validation.VisUserRequestCommands;

/**
 * The support bot operator, in the {@code reviewSkillSuggestion} command, decided to ignore the user (in every command:
 * the ignoring is global), given the
 * {@code email} and the {@code skill} of the suggestion being reviewed.
 *
 * <p>Records the user in {@link VisEntityCommandNotAllowedToUser}, with the suggestion ({@code skill},
 * {@code synonym} and {@code description}) in the {@code description}, so that the next suggestions of the user no
 * longer reach the operator. Then discards the suggestion, without any decision and without notifying the user.
 */
public class VisBusinessSkillSuggestionIgnoreUser implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSkillSuggestionIgnoreUser INSTANCE = new VisBusinessSkillSuggestionIgnoreUser();

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessSkillSuggestionIgnoreUser() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the session, with the suggestion key
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation suggestionKey = json.getJsonPiece(VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill);
		boolean suggestionIsPending = VisEntitySkillPending.ENTITY.exists(suggestionKey);
		CcpJsonRepresentation storedSuggestion = suggestionIsPending ? VisEntitySkillPending.ENTITY.getOneById(suggestionKey) : CcpOtherConstants.EMPTY_JSON;
		CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(suggestionKey);
		CcpJsonRepresentation description = completeSuggestion.getJsonPiece(VisEntitySkillPending.Fields.skill, VisEntitySkillPending.Fields.synonym, VisEntitySkillPending.Fields.description);

		String email = suggestionKey.getAsString(VisEntitySkillPending.Fields.email);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithCommand = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, VisUserRequestCommands.reviewSkillSuggestion);
		CcpJsonRepresentation ignoredUser = ignoredUserWithCommand.put(VisEntityCommandNotAllowedToUser.Fields.description, description);
		VisEntityCommandNotAllowedToUser.ENTITY.save(ignoredUser);

		if(suggestionIsPending) {
			VisEntitySkillPending.ENTITY.delete(completeSuggestion);
		}

		return json;
	}
}
