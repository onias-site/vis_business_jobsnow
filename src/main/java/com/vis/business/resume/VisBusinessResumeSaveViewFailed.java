package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResumeViewFailed;

public class VisBusinessResumeSaveViewFailed implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	private VisBusinessResumeSaveViewFailed() {}
	
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", "errorDetails", "status");
		CcpJsonRepresentation put = json.put("status", status);
		VisEntityResumeViewFailed.ENTITY.createOrUpdate(put);
		return json;
	}

}
