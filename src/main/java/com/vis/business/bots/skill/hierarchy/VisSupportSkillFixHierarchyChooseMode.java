package com.vis.business.bots.skill.hierarchy;

import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyReview;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;

/**
 * Second step of the {@code fixSkillHierarchy} command: the operator approves all the pending items, rejects all the
 * pending items (both with one justification, that goes to every item) or chooses to decide them one by one.
 * The items decided in earlier reviews are already in {@code reviewDecisions} and keep their decisions.
 *
 * <p>Deciding all the items finishes the review ({@code reviewFinished}). Choosing one by one asks for the
 * first item and goes on to the next step. Choosing to ignore the user asks for the confirmation
 * ({@code ignoreConfirmationAsked}). Any other answer, including a decision without justification, is
 * asked again ({@code invalidAnswer}). A request the user withdrew after it was shown ends the session with a notice
 * to the operator ({@code requestWithdrawn}), before the answer is read.
 */
public class VisSupportSkillFixHierarchyChooseMode implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSkillFixHierarchyChooseMode INSTANCE = new VisSupportSkillFixHierarchyChooseMode();

	private VisSupportSkillFixHierarchyChooseMode() {}

	/**
	 * Handles the choice of the operator.
	 * @param json the session, with the items to review
	 * @return the session asking for the first item, when the operator chose one by one
	 * @throws CcpErrorFlowDisturb with {@code reviewFinished}, {@code ignoreConfirmationAsked}, {@code invalidAnswer} or {@code requestWithdrawn}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		VisSupportSkillFixHierarchyConversation.endIfTheRequestWasWithdrawn(json);

		VisSupportSkillFixHierarchyAnswer answer = VisSupportSkillFixHierarchyAnswer.read(json);

		if(answer.isOneByOne()) {
			String firstItemPrompt = VisSupportSkillFixHierarchyConversation.getItemPrompt(json, 0);
			CcpJsonRepresentation jsonWithPrompt = json.put(JnJsonInstantMessengerFields.botReply, firstItemPrompt);
			return jsonWithPrompt;
		}

		if(answer.isIgnore()) {
			String ignoreConfirmation = VisSupportSkillFixHierarchyConversation.getIgnoreConfirmation(json);
			CcpJsonRepresentation jsonWithConfirmation = json.put(JnJsonInstantMessengerFields.botReply, ignoreConfirmation);
			CcpJsonRepresentation confirmationAsked = VisSupportSkillFixHierarchyStatus.ignoreConfirmationAsked.throwException(jsonWithConfirmation);
			return confirmationAsked;
		}

		boolean decisionWithJustification = answer.isDecisionWithJustification();

		if(decisionWithJustification) {
			List<CcpJsonRepresentation> reviewItems = json.getAsJsonList(VisSupportSkillFixHierarchyFields.reviewItems);
			// starts from the items decided in earlier reviews, which the first step already put in the session
			List<CcpJsonRepresentation> previousDecisions = json.getAsJsonList(VisSkillFixHierarchyReviewFields.reviewDecisions);
			List<CcpJsonRepresentation> decisions = new ArrayList<>(previousDecisions);

			for (CcpJsonRepresentation reviewItem : reviewItems) {
				String type = reviewItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
				String skill = reviewItem.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
				CcpJsonRepresentation decision = VisBusinessSkillFixHierarchyReview.getDecision(type, skill, answer.getDecision(), answer.justification);
				decisions.add(decision);
			}

			CcpJsonRepresentation finished = VisSupportSkillFixHierarchyConversation.finish(json, decisions);
			return finished;
		}

		JnLanguage language = VisSupportSkillFixHierarchyConversation.getLanguage(json);
		String notUnderstood = VisSupportSkillFixHierarchyConversation.getNotUnderstood(language);
		String options = VisSupportSkillFixHierarchyConversation.getOptions(language);
		String askAgain = notUnderstood + options;
		CcpJsonRepresentation jsonAskingAgain = json.put(JnJsonInstantMessengerFields.botReply, askAgain);
		CcpJsonRepresentation askedAgain = VisSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}
