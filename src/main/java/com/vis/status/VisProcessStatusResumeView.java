package com.vis.status;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.process.CcpProcessStatus;
import com.vis.entities.VisEntityResumeViewFailed;

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
		CcpBulkItem bulkItem = VisEntityResumeViewFailed.ENTITY.toBulkItem(json, CcpBulkEntityOperationType.create);
		return bulkItem;
	}
}
