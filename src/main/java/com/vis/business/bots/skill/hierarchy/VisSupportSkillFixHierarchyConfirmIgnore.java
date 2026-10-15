package com.vis.business.bots.skill.hierarchy;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyIgnoreUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Step of the {@code fixSkillHierarchy} command that confirms the intention of the operator to ignore the user. Yes
 * records the user in {@code VisEntityCommandNotAllowedToUser}, discards the request without notifying the user and ends
 * the session ({@code userIgnored}); no goes back to the choice of how to decide the items ({@code ignoreCanceled}); any
 * other answer asks again ({@code invalidAnswer}).
 */
public class VisSupportSkillFixHierarchyConfirmIgnore implements CcpBusiness {

	/**
	 * Handles the confirmation.
	 * @param json the session
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code userIgnored}, {@code ignoreCanceled} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		VisSupportSkillFixHierarchyAnswer answer = VisSupportSkillFixHierarchyAnswer.read(json);
		JnLanguage language = VisSupportSkillFixHierarchyConversation.getLanguage(json);

		if(answer.isYes()) {
			VisBusinessSkillFixHierarchyIgnoreUser.INSTANCE.execute(json);

			CcpJsonRepresentation userIgnoredParameters = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent);
			String userIgnoredText = VisSupportSkillFixHierarchyMessages.userIgnored.getMessage(language, userIgnoredParameters);
			CcpJsonRepresentation jsonWithReply = json.put(JnJsonInstantMessengerFields.botReply, userIgnoredText);
			CcpJsonRepresentation userIgnored = VisSupportSkillFixHierarchyStatus.userIgnored.throwException(jsonWithReply);
			return userIgnored;
		}

		if(answer.isNo()) {
			String options = VisSupportSkillFixHierarchyConversation.getOptions(language);
			CcpJsonRepresentation ignoreCanceledParameters = CcpOtherConstants.EMPTY_JSON.put(VisSupportSkillFixHierarchyMessageFields.options, options);
			String backToOptions = VisSupportSkillFixHierarchyMessages.ignoreCanceled.getMessage(language, ignoreCanceledParameters);
			CcpJsonRepresentation jsonWithReply = json.put(JnJsonInstantMessengerFields.botReply, backToOptions);
			CcpJsonRepresentation ignoreCanceled = VisSupportSkillFixHierarchyStatus.ignoreCanceled.throwException(jsonWithReply);
			return ignoreCanceled;
		}

		String ignoreConfirmation = VisSupportSkillFixHierarchyConversation.getIgnoreConfirmation(json);
		CcpJsonRepresentation askAgainParameters = CcpOtherConstants.EMPTY_JSON.put(VisSupportSkillFixHierarchyMessageFields.ignoreConfirmation, ignoreConfirmation);
		String askAgain = VisSupportSkillFixHierarchyMessages.ignoreConfirmationNotUnderstood.getMessage(language, askAgainParameters);
		CcpJsonRepresentation jsonAskingAgain = json.put(JnJsonInstantMessengerFields.botReply, askAgain);
		CcpJsonRepresentation askedAgain = VisSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}
