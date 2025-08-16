package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.vis.entities.VisEntityResumeViewFailed;

enum VisBusinessResumeSaveViewFailedConstants  implements CcpJsonFieldName{
	status, errorDetails
	
}

public class VisBusinessResumeSaveViewFailed implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	private VisBusinessResumeSaveViewFailed() {}
	
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", VisBusinessResumeSaveViewFailedConstants.errorDetails, VisBusinessResumeSaveViewFailedConstants.status);
		CcpJsonRepresentation put = json.put(VisBusinessResumeSaveViewFailedConstants.status, status);
		VisEntityResumeViewFailed.ENTITY.createOrUpdate(put);
		return json;
	}

}
