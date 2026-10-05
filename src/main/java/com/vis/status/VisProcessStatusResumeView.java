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
	/** Status 301: the resume is inactive. */
	inactiveResume(301),
	/** Status 402: the balance of the recruiter does not cover the fee. */
	insufficientFunds(402),
	/** Status 404: the resume does not exist. */
	resumeNotFound(404),
	/** Status 420: the candidate denied the view to the company of the recruiter. */
	notAllowedRecruiter(420),
	/** Status 427: the frequency has no fee. */
	missingFee(427), 
	/** Status 423: the recruiter has no balance. */
	missingBalance(423), 
	/** Status 0: the recruiter gave the resume a negative opinion. */
	negativatedResume(0), 
	;
	
	/** The HTTP status code. */
	final int status;
	
	/**
	 * Associates the HTTP status code.
	 * @param status the HTTP status code
	 */
	private VisProcessStatusResumeView(int status) {
		this.status = status;
	}

	/**
	 * Returns the HTTP status code.
	 * @return the status code
	 */
	public int asNumber() {
		return this.status;
	}

	/**
	 * Builds the item that records the failed view in {@code vis_resume_view_failed}.
	 * @param json the recruiter and resume keys
	 * @return the {@code create} bulk item
	 */
	public CcpBulkItem toBulkItemCreate(CcpJsonRepresentation json) {
		String recordId = VisEntityResumeViewFailed.ENTITY.calculateId(json);
		CcpBulkItem bulkItem = new CcpBulkItem(json, CcpBulkEntityOperationType.create, VisEntityResumeViewFailed.ENTITY, recordId);
		return bulkItem;
	}
}
