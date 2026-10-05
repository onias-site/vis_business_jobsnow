package com.vis.services;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillFixHierarchyFulfiled;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Where a hierarchy fix suggestion can be, in the order in which
 * {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy} looks for it. The name of each item is the value
 * returned in the {@code status} field.
 */
enum VisSkillFixHierarchyStatus {
	/** Waiting for review. */
	pending(VisEntitySkillFixHierarchyPending.ENTITY),
	/** Reviewed. */
	fulfiled(VisEntitySkillFixHierarchyFulfiled.ENTITY),
	;

	/** The entity where the suggestion is in this status. */
	final CcpEntity entity;

	/**
	 * Associates the status with its entity.
	 * @param entity the entity
	 */
	private VisSkillFixHierarchyStatus(CcpEntity entity) {
		this.entity = entity;
	}
}
