package com.vis.business.bots.skill.suggestion;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.bots.skill.hierarchy.VisSupportSkillFixHierarchyAnswer;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillSuggestionReview;
import com.vis.business.skill.VisSkillSuggestionDecisions;
import com.vis.business.skill.VisSkillSuggestionReviewFields;
import com.vis.entities.VisEntitySkillPending;

/**
 * Second step of the {@code reviewSkillSuggestion} command: the operator approves or rejects the skill, always with a
 * justification of 10 to 500 characters (it goes to the user in the email and is kept in the {@code explanation} of
 * the reviewed suggestion), or asks to ignore the user.
 *
 * <p>A decision moves the suggestion to {@code VisEntitySkillApproved} or {@code VisEntitySkillRejected}
 * ({@link VisBusinessSkillSuggestionReview}) and finishes the review ({@code reviewFinished}); a suggestion that left
 * the pending entity meanwhile (withdrawn by the user) ends the session with {@code requestNotFound}. Choosing to
 * ignore the user asks for the confirmation ({@code ignoreConfirmationAsked}). Any other answer, including a decision
 * without a valid justification, is asked again ({@code invalidAnswer}).
 */
public class VisSupportSkillSuggestionDecide implements CcpBusiness {

	/**
	 * Handles the answer of the operator.
	 * @param json the session, with {@code email} and {@code skill}
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code reviewFinished}, {@code requestNotFound}, {@code ignoreConfirmationAsked} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		VisSupportSkillFixHierarchyAnswer answer = VisSupportSkillFixHierarchyAnswer.read(json);

		if(answer.isIgnore()) {
			String ignoreConfirmation = VisSupportSkillSuggestionConversation.getIgnoreConfirmation(json);
			CcpJsonRepresentation jsonWithConfirmation = VisSupportSkillSuggestionConversation.reply(json, ignoreConfirmation);
			CcpJsonRepresentation confirmationAsked = VisSupportSkillSuggestionStatus.ignoreConfirmationAsked.throwException(jsonWithConfirmation);
			return confirmationAsked;
		}

		int justificationLength = answer.justification.length();
		boolean justificationHasValidLength = justificationLength >= 10 && justificationLength <= 500;
		boolean decisionWithValidJustification = answer.isDecisionWithJustification() && justificationHasValidLength;

		if(false == decisionWithValidJustification) {
			String notUnderstood = VisSupportSkillSuggestionConversation.getNotUnderstood(json);
			CcpJsonRepresentation jsonAskingAgain = VisSupportSkillSuggestionConversation.reply(json, notUnderstood);
			CcpJsonRepresentation askedAgain = VisSupportSkillSuggestionStatus.invalidAnswer.throwException(jsonAskingAgain);
			return askedAgain;
		}

		CcpJsonRepresentation suggestionKey = json.getJsonPiece(VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill);
		boolean suggestionIsNotPending = false == VisEntitySkillPending.ENTITY.exists(suggestionKey);

		if(suggestionIsNotPending) {
			CcpJsonRepresentation notFound = VisSupportSkillSuggestionStatus.requestNotFound.throwException(json);
			return notFound;
		}

		boolean isApproval = answer.isApproval();
		VisSkillSuggestionDecisions decision = isApproval ? VisSkillSuggestionDecisions.approved : VisSkillSuggestionDecisions.rejected;
		CcpJsonRepresentation reviewWithDecision = suggestionKey.put(VisSkillSuggestionReviewFields.decision, decision.name());
		CcpJsonRepresentation review = reviewWithDecision.put(JnJsonCommonsFields.explanation, answer.justification);
		VisBusinessSkillSuggestionReview.INSTANCE.execute(review);

		JnLanguage language = VisSupportSkillSuggestionConversation.getLanguage(json);
		VisSupportSkillSuggestionMessages finishedMessage = isApproval ? VisSupportSkillSuggestionMessages.approved : VisSupportSkillSuggestionMessages.rejected;
		String finishedText = finishedMessage.getMessage(language, suggestionKey);
		CcpJsonRepresentation jsonWithReply = VisSupportSkillSuggestionConversation.reply(json, finishedText);
		CcpJsonRepresentation finished = VisSupportSkillSuggestionStatus.reviewFinished.throwException(jsonWithReply);
		return finished;
	}
}
