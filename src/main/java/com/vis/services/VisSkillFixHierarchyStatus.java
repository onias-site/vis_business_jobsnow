package com.vis.services;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillFixHierarchyRejected;

/**
 * Where a hierarchy fix suggestion can be, in the order in which
 * {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy} looks for it. The name of each item is the value
 * returned in the {@code status} field.
 */
enum VisSkillFixHierarchyStatus {
	pending(VisEntitySkillFixHierarchyPending.ENTITY),
	approved(VisEntitySkillFixHierarchyApproved.ENTITY),
	rejected(VisEntitySkillFixHierarchyRejected.ENTITY),
	;

	final CcpEntity entity;

	private VisSkillFixHierarchyStatus(CcpEntity entity) {
		this.entity = entity;
	}
}
