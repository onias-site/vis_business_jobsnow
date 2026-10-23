package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Action of {@code VisBusinessSkillFixHierarchyIgnoreUser}, run for each type with a pending request: collects the
 * stored request, complete, with the readable email of the key in place of the stored hash.
 */
class VisBusinessCollectPendingFixHierarchyRequest implements CcpBusiness {

	/** Receives the complete pending requests. */
	private final List<CcpJsonRepresentation> pendingRequests;

	/**
	 * Keeps the list that receives the requests.
	 * @param pendingRequests receives the complete pending requests
	 */
	VisBusinessCollectPendingFixHierarchyRequest(List<CcpJsonRepresentation> pendingRequests) {
		this.pendingRequests = pendingRequests;
	}

	/**
	 * Collects the stored request.
	 * @param requestWithTheStoredRecord the key of the request, holding the stored record under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation requestWithTheStoredRecord) {
		CcpJsonRepresentation storedRequest = requestWithTheStoredRecord.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillFixHierarchyPending.ENTITY);
		CcpJsonRepresentation requestWithType = requestWithTheStoredRecord.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent, VisEntitySkillFixHierarchyPending.Fields.type);
		CcpJsonRepresentation completeRequest = storedRequest.mergeWithAnotherJson(requestWithType);
		this.pendingRequests.add(completeRequest);
		return requestWithTheStoredRecord;
	}
}
