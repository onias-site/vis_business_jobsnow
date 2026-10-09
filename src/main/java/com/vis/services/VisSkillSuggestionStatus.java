package com.vis.services;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillApproved;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;

/**
 * Where a skill suggestion can be, in the order in which {@link VisServiceSkillSuggestion#GetSkillSuggestion}
 * looks for it. The name of each item is the value returned in the {@code status} field.
 */
enum VisSkillSuggestionStatus {
	/** Waiting for review. */
	pending(VisEntitySkillPending.ENTITY),
	/** Approved by the support bot operator. */
	approved(VisEntitySkillApproved.ENTITY),
	/** Rejected by the support bot operator. */
	rejected(VisEntitySkillRejected.ENTITY),
	;

	/** The entity where the suggestion is in this status. */
	final CcpEntity entity;

	/**
	 * Associates the status with its entity.
	 * @param entity the entity
	 */
	private VisSkillSuggestionStatus(CcpEntity entity) {
		this.entity = entity;
	}
}
