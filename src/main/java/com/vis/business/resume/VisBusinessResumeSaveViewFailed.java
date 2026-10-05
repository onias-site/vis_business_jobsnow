package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityResumeViewFailed;
import com.jn.json.fields.validation.JnJsonCommonsFields;

import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * CcpBusiness implementation that persists the record of a failed attempt to view a resume.
 * Extracts the HTTP status from the errorDetails.status field and saves the record in the
 * VisEntityResumeViewFailed entity.
 */
public class VisBusinessResumeSaveViewFailed implements CcpBusiness {

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessResumeSaveViewFailed() {}
	
	/** The single instance. */
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", CcpJsonCommonsFields.errorDetails, JnJsonCommonsFields.status);
		CcpJsonRepresentation jsonWithStatus = json.put(JnJsonCommonsFields.status, status);
		VisEntityResumeViewFailed.ENTITY.save(jsonWithStatus);
		return json;
	}

}
