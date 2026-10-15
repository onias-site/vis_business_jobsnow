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
 * Step of the one by one review of the {@code fixSkillHierarchy} command: the operator approves or rejects the
 * current item, always with a justification. The step repeats itself (its {@code nextStep} is itself) while
 * there are items left, asking for the next one; the decision on the last item finishes the review
 * ({@code reviewFinished}). An answer that is not a decision with justification asks again for the same item
 * ({@code invalidAnswer}). A request the user withdrew in the middle of the review ends the session with a notice to
 * the operator ({@code requestWithdrawn}), before the answer is read.
 */
public class VisSupportSkillFixHierarchyDecideItem implements CcpBusiness {

	/**
	 * Records the decision on the current item and asks for the next one, or finishes the review after the last.
	 * @param json the session, with {@code itemIndex}
	 * @return the session asking for the next item
	 * @throws CcpErrorFlowDisturb with {@code reviewFinished}, {@code invalidAnswer} or {@code requestWithdrawn}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		VisSupportSkillFixHierarchyConversation.endIfTheRequestWasWithdrawn(json);

		int itemIndex = json.getAsIntegerNumber(VisSupportSkillFixHierarchyFields.itemIndex);
		VisSupportSkillFixHierarchyAnswer answer = VisSupportSkillFixHierarchyAnswer.read(json);
		boolean decisionWithJustification = answer.isDecisionWithJustification();

		if(false == decisionWithJustification) {
			JnLanguage language = VisSupportSkillFixHierarchyConversation.getLanguage(json);
			String notUnderstood = VisSupportSkillFixHierarchyConversation.getNotUnderstood(language);
			String sameItemPrompt = VisSupportSkillFixHierarchyConversation.getItemPrompt(json, itemIndex);
			String askAgain = notUnderstood + sameItemPrompt;
			CcpJsonRepresentation jsonAskingAgain = json.put(JnJsonInstantMessengerFields.botReply, askAgain);
			CcpJsonRepresentation askedAgain = VisSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
			return askedAgain;
		}

		List<CcpJsonRepresentation> reviewItems = json.getAsJsonList(VisSupportSkillFixHierarchyFields.reviewItems);
		CcpJsonRepresentation item = reviewItems.get(itemIndex);
		String type = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.type);
		String skill = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		CcpJsonRepresentation decision = VisBusinessSkillFixHierarchyReview.getDecision(type, skill, answer.getDecision(), answer.justification);

		List<CcpJsonRepresentation> previousDecisions = json.getAsJsonList(VisSkillFixHierarchyReviewFields.reviewDecisions);
		List<CcpJsonRepresentation> decisions = new ArrayList<>(previousDecisions);
		decisions.add(decision);

		int nextItemIndex = itemIndex + 1;
		boolean isLastItem = nextItemIndex >= reviewItems.size();

		if(isLastItem) {
			CcpJsonRepresentation finished = VisSupportSkillFixHierarchyConversation.finish(json, decisions);
			return finished;
		}

		CcpJsonRepresentation jsonWithDecisions = json.put(VisSkillFixHierarchyReviewFields.reviewDecisions, decisions);
		CcpJsonRepresentation jsonWithNextIndex = jsonWithDecisions.put(VisSupportSkillFixHierarchyFields.itemIndex, nextItemIndex);
		String nextItemPrompt = VisSupportSkillFixHierarchyConversation.getItemPrompt(jsonWithNextIndex, nextItemIndex);
		CcpJsonRepresentation jsonWithPrompt = jsonWithNextIndex.put(JnJsonInstantMessengerFields.botReply, nextItemPrompt);
		return jsonWithPrompt;
	}
}
