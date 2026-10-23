package com.vis.business.skill;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;
import com.vis.json.fields.validation.VisUserRequestCommands;

/**
 * The support bot operator, in the {@code fixSkillHierarchy} command, decided to ignore the user (in every command:
 * the ignoring is global), given the
 * {@code email} and the {@code parent} of the request being reviewed.
 *
 * <p>Records the user in {@link VisEntityCommandNotAllowedToUser}, with the pending requests for that parent
 * (both types) in the {@code description}, so that the next requests of the user no longer reach the operator.
 * Saving the user discards these requests and every other request of the user still pending, as if they had never
 * existed ({@code VisBusinessDiscardPendingRequestsOfIgnoredUser}), without any decision and without notifying the
 * user: nothing is approved or rejected, and the items already decided in earlier reviews stay as they are.
 */
public class VisBusinessSkillFixHierarchyIgnoreUser implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSkillFixHierarchyIgnoreUser INSTANCE = new VisBusinessSkillFixHierarchyIgnoreUser();

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessSkillFixHierarchyIgnoreUser() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
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
		CcpJsonRepresentation descriptionWithRequests = descriptionWithParent.put(JsonFieldNames.requests, requestsDescriptions);

		String email = requestKey.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
		// the email of the record is kept as hash; the readable one goes in the description, for the operator to list the ignored users
		CcpJsonRepresentation description = descriptionWithRequests.put(VisEntitySkillFixHierarchyPending.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		CcpJsonRepresentation ignoredUserWithCommand = ignoredUserWithEmail.put(VisEntityCommandNotAllowedToUser.Fields.commandName, VisUserRequestCommands.fixSkillHierarchy);
		CcpJsonRepresentation ignoredUser = ignoredUserWithCommand.put(VisEntityCommandNotAllowedToUser.Fields.description, description);
		// saving the user discards these requests and every other pending request of the user
		VisEntityCommandNotAllowedToUser.ENTITY.save(ignoredUser);
		return json;
	}

	/**
	 * The pending requests of both types for the email and the parent, complete (the {@code skill} array tells
	 * the deletion which items may have become orphans), with the readable email of the key in place of the
	 * stored hash. Both types are read by id in a single union all.
	 */
	private List<CcpJsonRepresentation> getPendingRequests(CcpJsonRepresentation requestKey) {

		VisSkillFixHierarchyTypes[] types = VisSkillFixHierarchyTypes.values();
		int typesCount = types.length;
		CcpJsonRepresentation[] requestKeys = new CcpJsonRepresentation[typesCount];

		for (int index = 0; index < typesCount; index++) {
			VisSkillFixHierarchyTypes type = types[index];
			requestKeys[index] = requestKey.put(VisEntitySkillFixHierarchyPending.Fields.type, type);
		}

		List<CcpJsonRepresentation> pendingRequests = new ArrayList<>();
		CcpBusiness collectTheRequest = new VisBusinessCollectPendingFixHierarchyRequest(pendingRequests);

		// the action runs only for the types with a pending request
		new CcpGetEntityId(requestKeys)
		.toBeginProcedureAnd()
			.forEachIdPresentInEntity(VisEntitySkillFixHierarchyPending.ENTITY).executeAction(collectTheRequest)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;

		return pendingRequests;
	}

	/**
	 * Field of the {@code description} of {@link VisEntityCommandNotAllowedToUser}: the ignored requests, each
	 * one with its {@code type}, the user's {@code description} and the {@code skill} array. Also holds the field
	 * returned by the reading procedure, which answers nothing.
	 */
	public static enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code requests} field. */
		requests,
		/** The {@code inexistentField} field: the reading procedure returns no field. */
		inexistentField
	}
}
