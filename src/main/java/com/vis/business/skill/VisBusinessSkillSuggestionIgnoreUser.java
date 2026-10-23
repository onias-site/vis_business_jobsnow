package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillPending;

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

		CcpBusiness saveTheIgnoredUser = new VisBusinessSaveUserIgnoredInSkillSuggestion(suggestionKey);

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
