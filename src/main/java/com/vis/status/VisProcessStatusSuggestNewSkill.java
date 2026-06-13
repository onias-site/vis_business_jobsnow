package com.vis.status;

import com.ccp.process.CcpProcessStatus;

/**
 * Define os status de processo para a operação de sugestão de nova skill, com seus códigos HTTP
 * correspondentes.
 */
public enum VisProcessStatusSuggestNewSkill implements CcpProcessStatus{
	rejectedSkill(420),
	approvedSkill(200),
	pendingSkill(202), 
	alreadyExists(409),
	;
	
	final int status;
	
	private VisProcessStatusSuggestNewSkill(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}

}
