package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.vis.entities.VisEntityResumeViewFailed;


public class VisBusinessResumeSaveViewFailed implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
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
