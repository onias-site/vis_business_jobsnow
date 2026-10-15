package com.vis.business.bots.skill.hierarchy;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;

/**
 * What the operator typed in answer to a step of the {@code fixSkillHierarchy} command, in Portuguese or English: a
 * decision (approve or reject, with the Portuguese verbs aprovar, rejeitar or reprovar) followed by the justification;
 * one by one (um a um) to decide item by item; ignore (ignorar) to ignore the user for the command; and yes or no (sim,
 * nao with or without the accent) to confirm it. Case is ignored. Also read by the {@code reviewSkillSuggestion}
 * command, which takes the same decisions on a single skill.
 */
public final class VisSupportSkillFixHierarchyAnswer {

	/** The kind of answer. */
	final VisSupportSkillFixHierarchyAnswerType type;

	/** The justification of a decision; empty otherwise. */
	public final String justification;

	/**
	 * Builds the answer.
	 * @param type the kind of answer
	 * @param justification the justification
	 */
	private VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType type, String justification) {
		this.type = type;
		this.justification = justification;
	}

	/**
	 * Reads the answer from {@code typedValue}.
	 * @param json the session
	 * @return the answer ({@code notUnderstood} when it matches nothing)
	 */
	public static VisSupportSkillFixHierarchyAnswer read(CcpJsonRepresentation json) {

		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = typedValue.trim();
		String lowerCaseAnswer = answer.toLowerCase();

		boolean isOneByOne = lowerCaseAnswer.equals("um a um") || lowerCaseAnswer.equals("one by one");

		if(isOneByOne) {
			VisSupportSkillFixHierarchyAnswer oneByOne = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.oneByOne, "");
			return oneByOne;
		}

		boolean isIgnore = lowerCaseAnswer.equals("ignorar") || lowerCaseAnswer.equals("ignore");

		if(isIgnore) {
			VisSupportSkillFixHierarchyAnswer ignore = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.ignore, "");
			return ignore;
		}

		boolean isYes = lowerCaseAnswer.equals("sim") || lowerCaseAnswer.equals("yes");

		if(isYes) {
			VisSupportSkillFixHierarchyAnswer yes = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.yes, "");
			return yes;
		}

		boolean isNo = lowerCaseAnswer.equals("não") || lowerCaseAnswer.equals("nao") || lowerCaseAnswer.equals("no");

		if(isNo) {
			VisSupportSkillFixHierarchyAnswer no = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.no, "");
			return no;
		}

		String[] firstWordAndTheRest = answer.split("\\s+", 2);
		String firstWord = firstWordAndTheRest[0].toLowerCase();
		boolean hasTheRest = firstWordAndTheRest.length > 1;
		String justification = hasTheRest ? firstWordAndTheRest[1].trim() : "";

		boolean isApproval = firstWord.equals("aprovar") || firstWord.equals("approve");

		if(isApproval) {
			VisSupportSkillFixHierarchyAnswer approval = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.approve, justification);
			return approval;
		}

		boolean isRejection = firstWord.equals("rejeitar") || firstWord.equals("reprovar") || firstWord.equals("reject");

		if(isRejection) {
			VisSupportSkillFixHierarchyAnswer rejection = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.reject, justification);
			return rejection;
		}

		VisSupportSkillFixHierarchyAnswer notUnderstood = new VisSupportSkillFixHierarchyAnswer(VisSupportSkillFixHierarchyAnswerType.notUnderstood, "");
		return notUnderstood;
	}

	/**
	 * Tells whether the operator chose to decide item by item.
	 * @return {@code true} for one by one
	 */
	public boolean isOneByOne() {
		boolean oneByOne = VisSupportSkillFixHierarchyAnswerType.oneByOne == this.type;
		return oneByOne;
	}

	/**
	 * Tells whether the operator asked to ignore the user.
	 * @return {@code true} for ignore
	 */
	public boolean isIgnore() {
		boolean ignore = VisSupportSkillFixHierarchyAnswerType.ignore == this.type;
		return ignore;
	}

	/**
	 * Tells whether the operator confirmed.
	 * @return {@code true} for yes
	 */
	public boolean isYes() {
		boolean yes = VisSupportSkillFixHierarchyAnswerType.yes == this.type;
		return yes;
	}

	/**
	 * Tells whether the operator declined.
	 * @return {@code true} for no
	 */
	public boolean isNo() {
		boolean no = VisSupportSkillFixHierarchyAnswerType.no == this.type;
		return no;
	}

	/**
	 * A decision counts only with its justification: it goes to the user in the email.
	 */
	public boolean isDecisionWithJustification() {
		boolean isDecision = VisSupportSkillFixHierarchyAnswerType.approve == this.type || VisSupportSkillFixHierarchyAnswerType.reject == this.type;
		boolean hasJustification = false == this.justification.isEmpty();
		boolean decisionWithJustification = isDecision && hasJustification;
		return decisionWithJustification;
	}

	/**
	 * Tells whether the operator approved (with or without justification).
	 * @return {@code true} for an approval
	 */
	public boolean isApproval() {
		boolean approval = VisSupportSkillFixHierarchyAnswerType.approve == this.type;
		return approval;
	}

	/**
	 * The decision of an approval or of a rejection.
	 */
	VisSkillFixHierarchyDecisions getDecision() {
		boolean isApproval = VisSupportSkillFixHierarchyAnswerType.approve == this.type;
		VisSkillFixHierarchyDecisions decision = isApproval ? VisSkillFixHierarchyDecisions.approved : VisSkillFixHierarchyDecisions.rejected;
		return decision;
	}
}
