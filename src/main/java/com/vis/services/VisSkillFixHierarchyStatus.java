package com.vis.services;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillFixHierarchyRejected;

/**
 * Onde a sugestão de correção de hierarquia pode estar, na ordem em que
 * {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy} a procura. O nome de cada item é o valor
 * devolvido no campo {@code status}.
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
