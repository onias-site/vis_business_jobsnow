package com.vis.business.skill;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;
import com.vis.json.fields.validation.VisUserRequestCommands;

/**
 * The support bot operator decided to ignore the user for the {@code fixSkillHierarchy} command, given the
 * {@code email} and the {@code parent} of the request being reviewed.
 *
 * <p>Records the user in {@link VisEntityCommandNotAllowedToUser}, with the pending requests for that parent
 * (both types) in the {@code description}, so that the next requests of the user no longer reach the operator.
 * Then discards those requests and their items still pending, without any decision and without notifying the
 * user: nothing is approved or rejected, and the items already decided in earlier reviews stay as they are.
 */
public class VisBusinessSkillFixHierarchyIgnoreUser implements CcpBusiness {

	public static final VisBusinessSkillFixHierarchyIgnoreUser INSTANCE = new VisBusinessSkillFixHierarchyIgnoreUser();

	private VisBusinessSkillFixHierarchyIgnoreUser() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		CcpJsonRepresentation requestKey = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent);
		List<CcpJsonRepresentation> pendingRequests = this.getPendingRequests(requestKey);

		String parent = requestKey.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
		List<CcpJsonRepresentation> requestsDescriptions = new ArrayList<>();

		for (CcpJsonRepresentation pendingRequest : pendingRequests) {
			CcpJsonRepresentation requestDescription = pendingRequest.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.type, VisEntitySkillFixHierarchyPending.Fields.description, VisEntitySkillFixHierarchyPending.Fields.skill);
			requestsDescriptions.add(requestDescription);
		}

		CcpJsonRepresentation descriptionWithParent = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkillFixHierarchyPending.Fields.parent, parent);
		CcpJsonRepresentation description = descriptionWithParent.put(JsonFieldNames.requests, requestsDescriptions);

		String email = requestKey.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithCommand = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, VisUserRequestCommands.fixSkillHierarchy);
		CcpJsonRepresentation ignoredUser = ignoredUserWithCommand.put(VisEntityCommandNotAllowedToUser.Fields.description, description);
		VisEntityCommandNotAllowedToUser.ENTITY.save(ignoredUser);

		for (CcpJsonRepresentation pendingRequest : pendingRequests) {
			this.discardRequest(pendingRequest);
		}

		return json;
	}

	/**
	 * The pending requests of both types for the email and the parent, complete (the pending entity writes
	 * through the messaging, which validates the whole record, even to delete it), with the readable email
	 * of the key in place of the stored hash.
	 */
	private List<CcpJsonRepresentation> getPendingRequests(CcpJsonRepresentation requestKey) {

		VisSkillFixHierarchyTypes[] types = VisSkillFixHierarchyTypes.values();
		List<CcpJsonRepresentation> pendingRequests = new ArrayList<>();

		for (VisSkillFixHierarchyTypes type : types) {
			CcpJsonRepresentation requestWithType = requestKey.put(VisEntitySkillFixHierarchyPending.Fields.type, type);
			boolean requestIsNotPending = false == VisEntitySkillFixHierarchyPending.ENTITY.exists(requestWithType);

			if(requestIsNotPending) {
				continue;
			}

			CcpJsonRepresentation storedRequest = VisEntitySkillFixHierarchyPending.ENTITY.getOneById(requestWithType);
			CcpJsonRepresentation completeRequest = storedRequest.mergeWithAnotherJson(requestWithType);
			pendingRequests.add(completeRequest);
		}

		return pendingRequests;
	}

	private void discardRequest(CcpJsonRepresentation pendingRequest) {

		CcpJsonRepresentation itemKeyWithoutSkill = pendingRequest.getJsonPiece(VisEntitySkillFixHierarchyItemPending.Fields.email, VisEntitySkillFixHierarchyItemPending.Fields.parent, VisEntitySkillFixHierarchyItemPending.Fields.type);
		List<String> skills = pendingRequest.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);

		for (String skill : skills) {
			CcpJsonRepresentation itemKey = itemKeyWithoutSkill.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill);
			boolean itemIsNotPending = false == VisEntitySkillFixHierarchyItemPending.ENTITY.exists(itemKey);

			if(itemIsNotPending) {
				continue;
			}

			// a plain delete would move the item to the twin, which holds the rejected items
			VisEntitySkillFixHierarchyItemPending.ENTITY.deleteAnyWhere(itemKey);
		}

		VisEntitySkillFixHierarchyPending.ENTITY.delete(pendingRequest);
	}

	/**
	 * Field of the {@code description} of {@link VisEntityCommandNotAllowedToUser}: the ignored requests, each
	 * one with its {@code type}, the user's {@code description} and the {@code skill} array.
	 */
	public static enum JsonFieldNames implements CcpJsonFieldName{
		requests
	}
}
