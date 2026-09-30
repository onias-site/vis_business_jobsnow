package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Defines the process statuses of the skill hierarchy fix suggestion, with their corresponding HTTP codes:
 * {@code userNotAllowed} (403) when the support bot operator chose to ignore the user for the
 * {@code fixSkillHierarchy} command.
 */
public enum VisProcessStatusFixSkillHierarchy implements CcpProcessStatus{
	userNotAllowed(403),
	;

	final int status;

	private VisProcessStatusFixSkillHierarchy(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}

}
