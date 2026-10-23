package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntitySkillFixHierarchyFulfiled;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Action of {@code VisBusinessSkillFixHierarchyReview} when the request of a type is pending: moves it to
 * {@code vis_skill_fix_hierarchy_fulfiled}, with the decisions and their explanation.
 */
class VisBusinessTransferFixHierarchyRequestToFulfiled implements CcpBusiness {

	/** The key of the request with its type, with the readable email. */
	private final CcpJsonRepresentation requestWithType;

	/** The explanation of the decisions, one line per item. */
	private final String explanation;

	/** The decisions of the items of the type. */
	private final List<CcpJsonRepresentation> typeDecisions;

	/**
	 * Keeps what the request receives.
	 * @param requestWithType the key of the request with its type, with the readable email
	 * @param explanation the explanation of the decisions, one line per item
	 * @param typeDecisions the decisions of the items of the type
	 */
	VisBusinessTransferFixHierarchyRequestToFulfiled(CcpJsonRepresentation requestWithType, String explanation, List<CcpJsonRepresentation> typeDecisions) {
		this.requestWithType = requestWithType;
		this.explanation = explanation;
		this.typeDecisions = typeDecisions;
	}

	/**
	 * Transfers the request to the fulfiled ones.
	 * @param jsonWithTheStoredRequest the json of the procedure, holding the request under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredRequest) {
		// the pending entity writes through the messaging, which validates the whole record (description and
		// skill are required); the stored email is the hash, so the readable one from the request key prevails
		CcpJsonRepresentation storedRequest = jsonWithTheStoredRequest.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkillFixHierarchyPending.ENTITY);
		CcpJsonRepresentation completeRequest = storedRequest.mergeWithAnotherJson(this.requestWithType);
		CcpJsonRepresentation requestWithExplanation = completeRequest.put(JnJsonCommonsFields.explanation, this.explanation);
		CcpJsonRepresentation requestWithDecisions = requestWithExplanation.put(VisSkillFixHierarchyReviewFields.reviewDecisions, this.typeDecisions);

		VisEntitySkillFixHierarchyPending.ENTITY.transferDataTo(requestWithDecisions, VisEntitySkillFixHierarchyFulfiled.ENTITY);
		return jsonWithTheStoredRequest;
	}
}
