package com.vis.status;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.process.CcpProcessStatus;
import com.vis.entities.VisEntityResumeViewFailed;

/**
 * Defines all the process statuses of the resume viewing operation, each one with its corresponding
 * HTTP code. Used by the matching system to record viewing failures in
 * VisEntityResumeViewFailed.
 */
public enum VisProcessStatusResumeView implements CcpProcessStatus{
	inactiveResume(301),
	insufficientFunds(402),
	resumeNotFound(404),
	notAllowedRecruiter(420),
	missingFee(427), 
	missingBalance(423), 
	negativatedResume(0), 
	;
	
	final int status;
	
	private VisProcessStatusResumeView(int status) {
		this.status = status;
	}

	public int asNumber() {
		return this.status;
	}

	public CcpBulkItem toBulkItemCreate(CcpJsonRepresentation json) {
		String recordId = VisEntityResumeViewFailed.ENTITY.calculateId(json);
		CcpBulkItem bulkItem = new CcpBulkItem(json, CcpBulkEntityOperationType.create, VisEntityResumeViewFailed.ENTITY, recordId);
		return bulkItem;
	}
}
