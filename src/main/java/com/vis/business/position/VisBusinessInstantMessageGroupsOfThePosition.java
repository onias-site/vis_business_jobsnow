package com.vis.business.position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityInstantMessageGroup;
import com.vis.entities.VisEntityPositionInstantMessageGroup;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The instant message groups where a position would be published (since 2026-11-23), for the modal of the position form:
 * the records of {@code vis_position_instant_message_group} of every seniority and ddd of the position, in its area, read
 * by id; then the name and the link of each chat found, from {@code vis_instant_message_group}, also by id (MGIE, no
 * complex query). A chat of several combinations comes once; the groups come sorted by name.
 */
public class VisBusinessInstantMessageGroupsOfThePosition implements CcpBusiness {

	/** The fields of the request and of the response. */
	public static enum Fields implements CcpJsonFieldName {
		/** The area of the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,

		/** The seniorities of the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 6)
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityType.class)
		seniority,

		/** The ddds of the position (10, home office, included). */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 68)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/** The response: the groups, each one with its name and link. */
		groups,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessInstantMessageGroupsOfThePosition INSTANCE = new VisBusinessInstantMessageGroupsOfThePosition();

	private VisBusinessInstantMessageGroupsOfThePosition() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Object resumeType = json.get(Fields.resumeType);
		List<String> seniorities = json.getAsStringList(Fields.seniority);
		List<Object> ddds = json.getAsObjectList(Fields.ddd);
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String seniority : seniorities) {
			for (Object ddd : ddds) {
				CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
						.put(VisEntityPositionInstantMessageGroup.Fields.seniority, seniority)
						.put(VisEntityPositionInstantMessageGroup.Fields.resumeType, resumeType)
						.put(VisEntityPositionInstantMessageGroup.Fields.ddd, ddd);
				combinations.add(combination);
			}
		}
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpJsonRepresentation[] combinationsArray = combinations.toArray(new CcpJsonRepresentation[0]);
		CcpSelectUnionAll combinationsFound = crud.unionAll(combinationsArray, JnDeleteKeysFromCache.INSTANCE, VisEntityPositionInstantMessageGroup.ENTITY);
		List<CcpJsonRepresentation> records = combinationsFound.getEntityRows(VisEntityPositionInstantMessageGroup.ENTITY);

		Set<Object> chats = new LinkedHashSet<>();
		for (CcpJsonRepresentation record : records) {
			List<Object> chatsOfTheRecord = record.getAsObjectList(VisEntityPositionInstantMessageGroup.Fields.chatId);
			chats.addAll(chatsOfTheRecord);
		}
		boolean noChat = chats.isEmpty();
		if (noChat) {
			CcpJsonRepresentation noGroups = CcpOtherConstants.EMPTY_JSON.put(Fields.groups, List.of());
			return noGroups;
		}
		List<CcpJsonRepresentation> chatKeys = new ArrayList<>();
		for (Object chat : chats) {
			CcpJsonRepresentation chatKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityInstantMessageGroup.Fields.chatId, chat);
			chatKeys.add(chatKey);
		}
		CcpJsonRepresentation[] chatKeysArray = chatKeys.toArray(new CcpJsonRepresentation[0]);
		CcpSelectUnionAll groupsFound = crud.unionAll(chatKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityInstantMessageGroup.ENTITY);
		List<CcpJsonRepresentation> groupRecords = groupsFound.getEntityRows(VisEntityInstantMessageGroup.ENTITY);
		List<CcpJsonRepresentation> groups = new ArrayList<>();
		for (CcpJsonRepresentation groupRecord : groupRecords) {
			CcpJsonRepresentation group = groupRecord.getJsonPiece(VisEntityInstantMessageGroup.Fields.title, VisEntityInstantMessageGroup.Fields.link);
			groups.add(group);
		}
		groups.sort(Comparator.comparing(group -> group.getAsString(VisEntityInstantMessageGroup.Fields.title)));
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.groups, groups);
		return response;
	}
}
