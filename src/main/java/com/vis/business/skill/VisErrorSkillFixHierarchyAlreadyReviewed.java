package com.vis.business.skill;

import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Every skill of a skill hierarchy fix request was already reviewed, for the same parent and type, in earlier
 * requests. Thrown by {@link VisBusinessSkillFixHierarchyRefuseAlreadyReviewed} and handled by the global
 * handlers of {@code VisEntitySkillFixHierarchyPending}, which email the user instead of saving the request.
 */
@SuppressWarnings("serial")
public class VisErrorSkillFixHierarchyAlreadyReviewed extends RuntimeException {
	VisErrorSkillFixHierarchyAlreadyReviewed(CcpJsonRepresentation request) {
		super("Every skill of the request was already reviewed in earlier requests: " + request);
	}
}
