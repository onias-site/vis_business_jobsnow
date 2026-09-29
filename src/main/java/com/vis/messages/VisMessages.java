package com.vis.messages;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnBulkCreateResult;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.jn.utils.JnSystemProperties;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

public class VisMessages {

	/**
	 * Email to the user when the request goes to {@code VisEntitySkillFixHierarchyApproved}, that is, when at least
	 * one of its items was approved. Lists the approved and the rejected items with the operator's justifications.
	 */
	public static class VisNotifyUserAboutAprovedSkillHierarchy implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation preparedJson = VisSkillFixHierarchyReviewMessage.prepare(json);
			return preparedJson;
		}
	}
	/**
	 * Email to the user when the request goes to {@code VisEntitySkillFixHierarchyRejected}, that is, when all of
	 * its items were rejected. Lists the rejected items with the operator's justifications.
	 */
	public static class VisNotifyUserAboutRejectedSkillHierarchy  implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation preparedJson = VisSkillFixHierarchyReviewMessage.prepare(json);
			return preparedJson;
		}
	}
	/**
	 * Before notifying about the pending request, splits it into one {@link VisEntitySkillFixHierarchyItemPending}
	 * record per skill of the {@code skill} array, created in a single bulk operation. An item that already
	 * exists as pending, as rejected (the twin) or in {@link VisEntitySkillFixHierarchyItemApproved} is not
	 * written. The json arrives already transformed by the pending entity (email as hash),
	 * which is the same form the item records keep.
	 * Returns the json with {@code skill} filtered down to the skills whose item was created now, so the
	 * message only mentions what is really new in the request, and with the readable email back in
	 * {@code email} (the transformer left the hash there), because the message goes to that address and
	 * the command sent to the support bot operator ({@code /fixSkillHierarchy {parent} {email}}) needs it.
	 */
	public static class VisNotifySupportAndUserAboutPendingSkillHierarchyRequest  implements CcpBusiness{

		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);
			Stream<String> skillsStream = skills.stream();
			Stream<String> distinctSkillsStream = skillsStream.distinct();
			Stream<CcpJsonRepresentation> itemsStream = distinctSkillsStream.map(skill -> json.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill));
			CcpJsonRepresentation[] items = itemsStream.toArray(CcpJsonRepresentation[]::new);

			CcpEntity rejectedItemEntity = VisEntitySkillFixHierarchyItemPending.ENTITY.getTwinEntity();
			CcpEntity[] entitiesThatPreventCreation = {rejectedItemEntity, VisEntitySkillFixHierarchyItemApproved.ENTITY};
			List<JnBulkCreateResult> results = JnExecuteBulkOperation.INSTANCE.executeCreateBulk(VisEntitySkillFixHierarchyItemPending.ENTITY, entitiesThatPreventCreation, JnDeleteKeysFromCache.INSTANCE, items);

			Stream<JnBulkCreateResult> resultsStream = results.stream();
			Stream<JnBulkCreateResult> createdResultsStream = resultsStream.filter(result -> result.created);
			Stream<String> createdSkillsStream = createdResultsStream.map(result -> result.json.getAsString(VisEntitySkillFixHierarchyItemPending.Fields.skill));
			List<String> createdSkills = createdSkillsStream.collect(Collectors.toList());

			CcpJsonRepresentation jsonWithCreatedSkills = json.put(VisEntitySkillFixHierarchyPending.Fields.skill, createdSkills);
			CcpJsonRepresentation jsonWithOriginalEmail = jsonWithCreatedSkills
					.renameField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail, JnJsonCommonsFields.email);

			String languageInTheJson = json.getAsString(JnJsonCommonsFields.language);
			boolean noLanguageInTheJson = languageInTheJson.isEmpty();
			String languageName = noLanguageInTheJson ? JnSystemProperties.INSTANCE.supportLanguage() : languageInTheJson;
			JnLanguage language = JnLanguage.valueOf(languageName);
			VisSkillFixHierarchyTypes type = json.getAsEnum(VisEntitySkillFixHierarchyPending.Fields.type, VisSkillFixHierarchyTypes.class);
			String typeDescription = type.getDescription(language);
			String skillNames = String.join(", ", createdSkills);

			CcpJsonRepresentation jsonWithTypeDescription = jsonWithOriginalEmail.put(JsonFieldNames.typeDescription, typeDescription);
			CcpJsonRepresentation jsonWithSkillNames = jsonWithTypeDescription.put(JsonFieldNames.skillNames, skillNames);
			return jsonWithSkillNames;
		}

		/**
		 * Placeholders of the email template that do not exist in the request: {@code typeDescription} is the
		 * {@code type} written in the language of the message ("associação"/"association" or
		 * "desassociação"/"dissociation"), resolved here because the template has no conditionals, in the same
		 * language that {@code JnMessageType.email} will use to pick the template (the one in the json, or the
		 * support language); {@code skillNames} is the filtered {@code skill} array joined by commas.
		 */
		public static enum JsonFieldNames implements CcpJsonFieldName{
			typeDescription,
			skillNames
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
