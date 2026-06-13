package com.vis.status;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.process.CcpProcessStatus;
import com.vis.entities.VisEntityResumeViewFailed;

/**
 * Define todos os status de processo para a operação de visualização de currículo, cada um com seu código
 * HTTP correspondente. Utilizado pelo sistema de matching para registrar falhas de visualização em
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
		CcpBulkItem bulkItem = new CcpBulkItem(json, CcpBulkEntityOperationType.create, VisEntityResumeViewFailed.ENTITY, VisEntityResumeViewFailed.ENTITY.calculateId(json));
		return bulkItem;
	}
}
