package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.utils.JnDeleteKeysFromCache;
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
 * longer reach the operator. Saving the user discards this suggestion and every other request of the user still
 * pending, as if they had never existed ({@code VisBusinessDiscardPendingRequestsOfIgnoredUser}), without any decision
 * and without notifying the user.
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

		CcpBusiness saveTheIgnoredUser = jsonWithTheStoredSuggestion -> {
			// empty when the suggestion is no longer pending
			CcpJsonRepresentation storedSuggestion = jsonWithTheStoredSuggestion.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillPending.ENTITY);
			CcpJsonRepresentation completeSuggestion = storedSuggestion.mergeWithAnotherJson(suggestionKey);
			CcpJsonRepresentation requestDescription = completeSuggestion.getJsonPiece(VisEntitySkillPending.Fields.skill, VisEntitySkillPending.Fields.synonym, VisEntitySkillPending.Fields.description);

			String email = suggestionKey.getAsString(VisEntitySkillPending.Fields.email);
			// the email of the record is kept as hash; the readable one goes in the description, for the operator to list the ignored users
			CcpJsonRepresentation description = requestDescription.put(VisEntitySkillPending.Fields.email, email);
			CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
			CcpJsonRepresentation ignoredUserWithCommand = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, VisUserRequestCommands.reviewSkillSuggestion);
			CcpJsonRepresentation ignoredUser = ignoredUserWithCommand.put(VisEntityCommandNotAllowedToUser.Fields.description, description);
			// saving the user discards this suggestion and every other pending request of the user
			VisEntityCommandNotAllowedToUser.ENTITY.save(ignoredUser);
			return jsonWithTheStoredSuggestion;
		};

		// a single read by id loads the pending suggestion, when there is one
		new CcpGetEntityId(suggestionKey)
		.toBeginProcedureAnd()
			.loadThisIdFromEntity(VisEntitySkillPending.ENTITY).and()
			.executeAction(saveTheIgnoredUser)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
		return json;
	}

	/** The procedure answers nothing: the business returns the json it received. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field. */
		inexistentField
	}
}
