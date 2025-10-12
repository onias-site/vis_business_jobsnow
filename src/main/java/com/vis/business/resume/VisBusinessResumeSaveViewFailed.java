package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityResumeViewFailed;

public class VisBusinessResumeSaveViewFailed implements CcpBusiness {
	enum JsonFieldNames implements CcpJsonFieldName{
		status, errorDetails
	}

	private VisBusinessResumeSaveViewFailed() {}
	
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", JsonFieldNames.errorDetails, JsonFieldNames.status);
		CcpJsonRepresentation put = json.put(JsonFieldNames.status, status);
		VisEntityResumeViewFailed.ENTITY.createOrUpdate(put);
		return json;
	}

}
