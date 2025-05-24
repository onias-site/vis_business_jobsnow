package com.vis.commons.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.commons.entities.VisEntityResumeViewFailed;

public class ResumeSaveViewFailed implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	private ResumeSaveViewFailed() {}
	
	public static final ResumeSaveViewFailed INSTANCE = new ResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", "errorDetails", "status");
		CcpJsonRepresentation put = json.put("status", status);
		VisEntityResumeViewFailed.ENTITY.createOrUpdate(put);
		return json;
	}

}
