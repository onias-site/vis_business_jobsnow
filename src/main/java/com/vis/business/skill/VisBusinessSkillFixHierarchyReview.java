package com.vis.business.skill;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillFixHierarchyRejected;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Applies the decisions taken by the support bot operator on a skill hierarchy fix request, given the
 * {@code email} and the {@code parent} of the request and the {@code reviewDecisions}.
 *
 * <p>Each item goes to {@link VisEntitySkillFixHierarchyItemApproved} when approved, or to the twin of
 * {@link VisEntitySkillFixHierarchyItemPending} (the rejected items) when rejected; an item that is no longer
 * pending (decided in an earlier review) stays where it is. Then the request of each
 * reviewed type leaves {@link VisEntitySkillFixHierarchyPending}: it goes to
 * {@link VisEntitySkillFixHierarchyApproved} when at least one of its items was approved, and to
 * {@link VisEntitySkillFixHierarchyRejected} when all of them were rejected. That transfer is what emails the
 * user, and the decisions travel with it so that the email lists the approved and the rejected items with the
 * operator's justifications. The operator's justifications are also kept in the {@code explanation} of the
 * request.
 */
public class VisBusinessSkillFixHierarchyReview implements CcpBusiness {

	public static final VisBusinessSkillFixHierarchyReview INSTANCE = new VisBusinessSkillFixHierarchyReview();

	private VisBusinessSkillFixHierarchyReview() {}

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

	private void moveRequest(CcpJsonRepresentation requestKey, VisSkillFixHierarchyTypes type, List<CcpJsonRepresentation> typeDecisions) {

		Stream<CcpJsonRepresentation> typeDecisionsStream = typeDecisions.stream();
		boolean anyItemApproved = typeDecisionsStream.anyMatch(decision -> VisSkillFixHierarchyDecisions.approved == decision.getAsEnum(VisSkillFixHierarchyReviewFields.decision, VisSkillFixHierarchyDecisions.class));
		CcpEntity targetEntity = anyItemApproved ? VisEntitySkillFixHierarchyApproved.ENTITY : VisEntitySkillFixHierarchyRejected.ENTITY;

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

		VisEntitySkillFixHierarchyPending.ENTITY.transferDataTo(requestWithDecisions, targetEntity);
	}

	private String getExplanationLine(CcpJsonRepresentation decision) {
		String skill = decision.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		String itemDecision = decision.getAsString(VisSkillFixHierarchyReviewFields.decision);
		String justification = decision.getAsString(VisSkillFixHierarchyReviewFields.justification);
		String explanationLine = skill + " (" + itemDecision + "): " + justification;
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
