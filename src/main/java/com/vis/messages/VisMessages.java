package com.vis.messages;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnRepeatableMessage;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

public class VisMessages {

	/**
	 * Email to the user when the request goes to {@code VisEntitySkillFixHierarchyApproved}, that is, when at least
	 * one of its items was approved. Lists the approved and the rejected items with the operator's justifications.
	 * Repeatable: each review is a new fact, and up to 2026-09-30 the second review of the same user in the same
	 * day was refused as a repetition, which broke the end of the review in the support bot.
	 */
	public static class VisNotifyUserAboutAprovedSkillHierarchy implements CcpBusiness, JnRepeatableMessage{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation preparedJson = VisSkillFixHierarchyReviewMessage.prepare(json);
			return preparedJson;
		}
	}
	/**
	 * Email to the user when the request goes to {@code VisEntitySkillFixHierarchyRejected}, that is, when all of
	 * its items were rejected. Lists the rejected items with the operator's justifications. Repeatable, for the
	 * same reason as {@link VisNotifyUserAboutAprovedSkillHierarchy}.
	 */
	public static class VisNotifyUserAboutRejectedSkillHierarchy  implements CcpBusiness, JnRepeatableMessage{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation preparedJson = VisSkillFixHierarchyReviewMessage.prepare(json);
			return preparedJson;
		}
	}
	/**
	 * Notice of a new pending request, to the user by email and to the support bot operator. Returns the json
	 * with the readable email back in {@code email} (the pending entity's transformer left the hash there),
	 * because the message goes to that address and the command sent to the support bot operator
	 * ({@code /fixSkillHierarchy {parent} {email}}) needs it. The items of the request are created by
	 * {@code VisBusinessSkillFixHierarchyCreateItems}, on every save.
	 *
	 * <p>Repeatable: every new request is a new fact, so the notice goes out even if the same user already had
	 * one today (the email) or the same parent within the hour (the command to the operator).
	 */
	public static class VisNotifySupportAndUserAboutPendingSkillHierarchyRequest  implements CcpBusiness, JnRepeatableMessage{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);
			Stream<String> skillsStream = skills.stream();
			Stream<String> distinctSkillsStream = skillsStream.distinct();
			List<String> distinctSkills = distinctSkillsStream.collect(Collectors.toList());

			CcpJsonRepresentation jsonWithOriginalEmail = json
					.renameField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail, JnJsonCommonsFields.email);

			String languageInTheJson = json.getAsString(JnJsonCommonsFields.language);
			boolean noLanguageInTheJson = languageInTheJson.isEmpty();
			String languageName = noLanguageInTheJson ? JnSystemProperties.INSTANCE.supportLanguage() : languageInTheJson;
			JnLanguage language = JnLanguage.valueOf(languageName);
			VisSkillFixHierarchyTypes type = json.getAsEnum(VisEntitySkillFixHierarchyPending.Fields.type, VisSkillFixHierarchyTypes.class);
			String typeDescription = type.getDescription(language);
			String skillNames = String.join(", ", distinctSkills);

			CcpJsonRepresentation jsonWithTypeDescription = jsonWithOriginalEmail.put(JsonFieldNames.typeDescription, typeDescription);
			CcpJsonRepresentation jsonWithSkillNames = jsonWithTypeDescription.put(JsonFieldNames.skillNames, skillNames);
			return jsonWithSkillNames;
		}

		/**
		 * Placeholders of the email template that do not exist in the request: {@code typeDescription} is the
		 * {@code type} written in the language of the message ("associação"/"association" or
		 * "desassociação"/"dissociation"), resolved here because the template has no conditionals, in the same
		 * language that {@code JnMessageType.email} will use to pick the template (the one in the json, or the
		 * support language); {@code skillNames} is the {@code skill} array, without repetitions, joined by commas.
		 */
		public static enum JsonFieldNames implements CcpJsonFieldName{
			typeDescription,
			skillNames
		}
	}
	/**
	 * Email to the user when a skill hierarchy fix request is refused because every one of its skills was already
	 * reviewed, for the same parent and type, in earlier requests. Uses the same placeholders as
	 * {@link VisNotifySupportAndUserAboutPendingSkillHierarchyRequest} ({@code typeDescription} and
	 * {@code skillNames}). Repeatable: every refused request is a new fact.
	 */
	public static class VisNotifyUserAboutAlreadyReviewedSkillHierarchy implements CcpBusiness, JnRepeatableMessage{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation preparedJson = new VisNotifySupportAndUserAboutPendingSkillHierarchyRequest().execute(json);
			return preparedJson;
		}
	}
	public static class VisNotifySupportAndUserAboutPendingSkillRequest implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			return json;
		}
	}
	public static class VisNotifyUserAboutAprovedSkill implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			return json;
		}
	}
	public static class VisNotifyUserAboutRejectedSkill implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			return json;
		}
	}


}
