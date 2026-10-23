package com.vis.business.bots.skill.suggestion;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.bots.skill.hierarchy.VisSupportSkillFixHierarchyAnswer;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillSuggestionIgnoreUser;
import com.vis.entities.VisEntitySkillPending;

/**
 * Step of the {@code reviewSkillSuggestion} command that confirms the intention of the operator to ignore the user.
 * Yes records the user in {@code VisEntityCommandNotAllowedToUser}, discards the suggestion without notifying the
 * user and ends the session ({@code userIgnored}); no goes back to the decision ({@code ignoreCanceled}); any other
 * answer asks again ({@code invalidAnswer}).
 */
public class VisSupportSkillSuggestionConfirmIgnore implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSkillSuggestionConfirmIgnore INSTANCE = new VisSupportSkillSuggestionConfirmIgnore();

	private VisSupportSkillSuggestionConfirmIgnore() {}

	/**
	 * Handles the confirmation.
	 * @param json the session
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code userIgnored}, {@code ignoreCanceled} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		VisSupportSkillFixHierarchyAnswer answer = VisSupportSkillFixHierarchyAnswer.read(json);
		JnLanguage language = VisSupportSkillSuggestionConversation.getLanguage(json);

		if(answer.isYes()) {
			VisBusinessSkillSuggestionIgnoreUser.INSTANCE.execute(json);
			CcpJsonRepresentation userIgnoredParameters = json.getJsonPiece(VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill);
			String userIgnoredText = VisSupportSkillSuggestionMessages.userIgnored.getMessage(language, userIgnoredParameters);
			CcpJsonRepresentation jsonWithReply = VisSupportSkillSuggestionConversation.reply(json, userIgnoredText);
			CcpJsonRepresentation userIgnored = VisSupportSkillSuggestionStatus.userIgnored.throwException(jsonWithReply);
			return userIgnored;
		}

		if(answer.isNo()) {
			String options = VisSupportSkillSuggestionConversation.getOptions(language);
			CcpJsonRepresentation ignoreCanceledParameters = CcpOtherConstants.EMPTY_JSON.put(VisSupportSkillSuggestionMessageFields.options, options);
			String backToOptions = VisSupportSkillSuggestionMessages.ignoreCanceled.getMessage(language, ignoreCanceledParameters);
			CcpJsonRepresentation jsonWithReply = VisSupportSkillSuggestionConversation.reply(json, backToOptions);
			CcpJsonRepresentation ignoreCanceled = VisSupportSkillSuggestionStatus.ignoreCanceled.throwException(jsonWithReply);
			return ignoreCanceled;
		}

		String ignoreConfirmation = VisSupportSkillSuggestionConversation.getIgnoreConfirmation(json);
		CcpJsonRepresentation askAgainParameters = CcpOtherConstants.EMPTY_JSON.put(VisSupportSkillSuggestionMessageFields.ignoreConfirmation, ignoreConfirmation);
		String askAgain = VisSupportSkillSuggestionMessages.ignoreConfirmationNotUnderstood.getMessage(language, askAgainParameters);
		CcpJsonRepresentation jsonAskingAgain = VisSupportSkillSuggestionConversation.reply(json, askAgain);
		CcpJsonRepresentation askedAgain = VisSupportSkillSuggestionStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}
