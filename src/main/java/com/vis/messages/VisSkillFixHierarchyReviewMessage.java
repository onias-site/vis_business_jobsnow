package com.vis.messages;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;
import com.vis.business.skill.VisSkillFixHierarchyDecisions;
import com.vis.business.skill.VisSkillFixHierarchyReviewFields;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Prepares the json of the email that tells the user the result of the review of a skill hierarchy fix
 * request, shared by the templates of the approved and of the rejected request. The json arrives from the
 * transfer of the request, already transformed (email as hash), and with the {@code reviewDecisions} taken by
 * the support bot operator.
 */
final class VisSkillFixHierarchyReviewMessage {

	private VisSkillFixHierarchyReviewMessage() {}

	/**
	 * Puts back the readable email (the address the email goes to), the type of the request written in the
	 * language of the message ({@code typeDescription}) and the list of approved and rejected items with the
	 * operator's justifications ({@code reviewSummary}). The language is the one {@code JnMessageType.email}
	 * will use to pick the template: the one in the json, or the support language.
	 */
	static CcpJsonRepresentation prepare(CcpJsonRepresentation json) {

		CcpJsonRepresentation jsonWithOriginalEmail = json
				.renameField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail, JnJsonCommonsFields.email);

		String languageInTheJson = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguageInTheJson = languageInTheJson.isEmpty();
		String languageName = noLanguageInTheJson ? JnSystemProperties.INSTANCE.supportLanguage() : languageInTheJson;
		JnLanguage language = JnLanguage.valueOf(languageName);

		VisSkillFixHierarchyTypes type = json.getAsEnum(VisEntitySkillFixHierarchyPending.Fields.type, VisSkillFixHierarchyTypes.class);
		String typeDescription = type.getDescription(language);

		List<CcpJsonRepresentation> decisions = json.getAsJsonList(VisSkillFixHierarchyReviewFields.reviewDecisions);
		String approvedItems = getItemsWithTheDecision(decisions, VisSkillFixHierarchyDecisions.approved, language);
		String rejectedItems = getItemsWithTheDecision(decisions, VisSkillFixHierarchyDecisions.rejected, language);
		String reviewSummary = approvedItems + rejectedItems;

		CcpJsonRepresentation jsonWithTypeDescription = jsonWithOriginalEmail.put(VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.JsonFieldNames.typeDescription, typeDescription);
		CcpJsonRepresentation jsonWithReviewSummary = jsonWithTypeDescription.put(VisSkillFixHierarchyReviewFields.reviewSummary, reviewSummary);
		return jsonWithReviewSummary;
	}

	private static String getItemsWithTheDecision(List<CcpJsonRepresentation> decisions, VisSkillFixHierarchyDecisions decision, JnLanguage language) {

		String decisionName = decision.name();
		Stream<CcpJsonRepresentation> decisionsStream = decisions.stream();
		Stream<CcpJsonRepresentation> itemsWithTheDecisionStream = decisionsStream.filter(item -> decisionName.equals(item.getAsString(VisSkillFixHierarchyReviewFields.decision)));
		Stream<String> listItemsStream = itemsWithTheDecisionStream.map(item -> getListItem(item));
		List<String> listItems = listItemsStream.collect(Collectors.toList());

		boolean noItemWithTheDecision = listItems.isEmpty();

		if(noItemWithTheDecision) {
			return "";
		}

		String title = decision.getTitle(language);
		String joinedListItems = String.join("", listItems);
		String itemsWithTheDecision = "<p>" + title + ":</p><ul>" + joinedListItems + "</ul>";
		return itemsWithTheDecision;
	}

	private static String getListItem(CcpJsonRepresentation item) {
		String skill = item.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill);
		String justification = item.getAsString(VisSkillFixHierarchyReviewFields.justification);
		String escapedSkill = escapeHtml(skill);
		String escapedJustification = escapeHtml(justification);
		String listItem = "<li><b>" + escapedSkill + "</b>: " + escapedJustification + "</li>";
		return listItem;
	}

	/**
	 * The justification is typed by the operator in the bot and goes into an HTML email body.
	 */
	private static String escapeHtml(String text) {
		String withoutAmpersand = text.replace("&", "&amp;");
		String withoutLessThan = withoutAmpersand.replace("<", "&lt;");
		String escaped = withoutLessThan.replace(">", "&gt;");
		return escaped;
	}
}
