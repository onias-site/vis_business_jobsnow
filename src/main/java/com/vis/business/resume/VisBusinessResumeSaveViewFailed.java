package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityResumeViewFailed;

/**
 * Implementação de CcpBusiness que persiste o registro de uma tentativa de visualização de currículo
 * que falhou. Extrai o status HTTP do campo errorDetails.status e salva o registro na entidade
 * VisEntityResumeViewFailed.
 */
public class VisBusinessResumeSaveViewFailed implements CcpBusiness {
	enum JsonFieldNames implements CcpJsonFieldName{
		status, errorDetails
	}

	private VisBusinessResumeSaveViewFailed() {}
	
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", JsonFieldNames.errorDetails, JsonFieldNames.status);
		CcpJsonRepresentation put = json.put(JsonFieldNames.status, status);
		VisEntityResumeViewFailed.ENTITY.save(put);
		return json;
	}

}
