package com.vis.business.skill;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;
import com.vis.entities.VisEntitySkillFixHierarchyFulfiled;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Applies the decisions taken by the support bot operator on a skill hierarchy fix request, given the
 * {@code email} and the {@code parent} of the request and the {@code reviewDecisions}.
 *
 * <p>Each item goes to {@link VisEntitySkillFixHierarchyItemApproved} when approved, or to the twin of
 * {@link VisEntitySkillFixHierarchyItemPending} (the rejected items) when rejected; an item that is no longer
 * pending (decided in an earlier review) stays where it is. Then the request of each
 * reviewed type leaves {@link VisEntitySkillFixHierarchyPending} for {@link VisEntitySkillFixHierarchyFulfiled},
 * whatever the decisions were (approve all, reject all or one by one). That transfer is what emails the
 * user, and the decisions travel with it so that the email lists the approved and the rejected items with the
 * operator's justifications. The operator's justifications are also kept in the {@code explanation} of the
 * request.
 */
public class VisBusinessSkillFixHierarchyReview implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSkillFixHierarchyReview INSTANCE = new VisBusinessSkillFixHierarchyReview();

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessSkillFixHierarchyReview() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation requestKey = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent);
		List<CcpJsonRepresentation> decisions = json.getAsJsonList(VisSkillFixHierarchyReviewFields.reviewDecisions);

		for (CcpJsonRepresentation decision : decisions) {
			this.moveItem(requestKey, decision);
		}

		VisSkillFixHierarchyTypes[] types = VisSkillFixHierarchyTypes.values();

		for (VisSkillFixHierarchyTypes type : types) {
			String typeName = type.name();
			Stream<CcpJsonRepresentation> decisionsStream = decisions.stream();
			Stream<CcpJsonRepresentation> typeDecisionsStream = decisionsStream.filter(decision -> typeName.equals(decision.getAsString(VisEntitySkillFixHierarchyPending.Fields.type)));
			List<CcpJsonRepresentation> typeDecisions = typeDecisionsStream.collect(Collectors.toList());

			boolean typeWasNotReviewed = typeDecisions.isEmpty();

			if(typeWasNotReviewed) {
				continue;
			}

			this.moveRequest(requestKey, type, typeDecisions);
		}

		return json;
	}

	/**
	 * Moves a pending item according to its decision: an approved one to {@code vis_skill_fix_hierarchy_item_approved}, a
	 * rejected one to the twin (by deleting it). An item no longer pending (decided in an earlier review) is left alone.
	 * @param requestKey the key of the request
	 * @param decision the decision of the item
	 */
	private void moveItem(CcpJsonRepresentation requestKey, CcpJsonRepresentation decision) {

		CcpJsonRepresentation itemKeyFields = decision.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.type, VisEntitySkillFixHierarchyItemPending.Fields.skill);
		CcpJsonRepresentation itemKey = requestKey.mergeWithAnotherJson(itemKeyFields);
		// an item decided in an earlier review is no longer pending: its decision is only reported to the user
		boolean itemIsNotPending = false == VisEntitySkillFixHierarchyItemPending.ENTITY.exists(itemKey);

		if(itemIsNotPending) {
			return;
		}

		VisSkillFixHierarchyDecisions itemDecision = decision.getAsEnum(VisSkillFixHierarchyReviewFields.decision, VisSkillFixHierarchyDecisions.class);

		boolean isApproved = VisSkillFixHierarchyDecisions.approved == itemDecision;

		if(isApproved) {
			VisEntitySkillFixHierarchyItemPending.ENTITY.transferDataTo(itemKey, VisEntitySkillFixHierarchyItemApproved.ENTITY);
			return;
		}

		// deleting from a twin entity moves the record to the twin, which holds the rejected items
		VisEntitySkillFixHierarchyItemPending.ENTITY.delete(itemKey);
	}

	/**
	 * Moves the pending request of the type to {@code vis_skill_fix_hierarchy_fulfiled}, with the decisions and their
	 * explanation (one line per item); a request no longer pending is left alone.
	 * @param requestKey the key of the request
	 * @param type the type of the request
	 * @param typeDecisions the decisions of the items of the type
	 */
	private void moveRequest(CcpJsonRepresentation requestKey, VisSkillFixHierarchyTypes type, List<CcpJsonRepresentation> typeDecisions) {

		Stream<CcpJsonRepresentation> decisionsToExplainStream = typeDecisions.stream();
		Stream<String> explanationLinesStream = decisionsToExplainStream.map(decision -> this.getExplanationLine(decision));
		String explanation = explanationLinesStream.collect(Collectors.joining("\n"));

		CcpJsonRepresentation requestWithType = requestKey.put(VisEntitySkillFixHierarchyPending.Fields.type, type);

		boolean requestIsNotPending = false == VisEntitySkillFixHierarchyPending.ENTITY.exists(requestWithType);

		if(requestIsNotPending) {
			return;
		}

		// the pending entity writes through the messaging, which validates the whole record (description and
		// skill are required); the stored email is the hash, so the readable one from the request key prevails
		CcpJsonRepresentation storedRequest = VisEntitySkillFixHierarchyPending.ENTITY.getOneById(requestWithType);
		CcpJsonRepresentation completeRequest = storedRequest.mergeWithAnotherJson(requestWithType);
		CcpJsonRepresentation requestWithExplanation = completeRequest.put(JnJsonCommonsFields.explanation, explanation);
		CcpJsonRepresentation requestWithDecisions = requestWithExplanation.put(VisSkillFixHierarchyReviewFields.reviewDecisions, typeDecisions);

		VisEntitySkillFixHierarchyPending.ENTITY.transferDataTo(requestWithDecisions, VisEntitySkillFixHierarchyFulfiled.ENTITY);
	}

	/**
	 * Builds the line {@code skill (decision): justification}, with the decision written in the language of the user.
	 * The language of the user is not stored yet, so it is the language of the system, as in the email of the review
	 * (until 2026-10-08 the line carried the name of the enum, {@code approved} or {@code rejected}, to a Portuguese
	 * reader).
	 * @param decision the decision of an item
	 * @return the line
	 */
	private String getExplanationLine(CcpJsonRepresentation decision) {
		String skill = decision.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		VisSkillFixHierarchyDecisionNames itemDecision = decision.getAsEnum(VisSkillFixHierarchyReviewFields.decision, VisSkillFixHierarchyDecisionNames.class);
		String languageName = JnSystemProperties.INSTANCE.supportLanguage();
		JnLanguage language = JnLanguage.valueOf(languageName);
		String decisionName = itemDecision.getName(language);
		String justification = decision.getAsString(VisSkillFixHierarchyReviewFields.justification);
		String explanationLine = skill + " (" + decisionName + "): " + justification;
		return explanationLine;
	}

	/**
	 * Builds one decision of {@code reviewDecisions}.
	 */
	public static CcpJsonRepresentation getDecision(String type, String skill, VisSkillFixHierarchyDecisions decision, String justification) {
		CcpJsonRepresentation decisionWithType = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkillFixHierarchyItemPending.Fields.type, type);
		CcpJsonRepresentation decisionWithSkill = decisionWithType.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill);
		CcpJsonRepresentation decisionWithDecision = decisionWithSkill.put(VisSkillFixHierarchyReviewFields.decision, decision.name());
		CcpJsonRepresentation decisionWithJustification = decisionWithDecision.put(VisSkillFixHierarchyReviewFields.justification, justification);
		return decisionWithJustification;
	}
}
