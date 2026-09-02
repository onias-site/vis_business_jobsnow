package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityResumeViewFailed;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Implementação de CcpBusiness que persiste o registro de uma tentativa de visualização de currículo
 * que falhou. Extrai o status HTTP do campo errorDetails.status e salva o registro na entidade
 * VisEntityResumeViewFailed.
 */
public class VisBusinessResumeSaveViewFailed implements CcpBusiness {
	enum JsonFieldNames implements CcpJsonFieldName{ errorDetails
	}

	private VisBusinessResumeSaveViewFailed() {}
	
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", JsonFieldNames.errorDetails, JnJsonCommonsFields.status);
		CcpJsonRepresentation put = json.put(JnJsonCommonsFields.status, status);
		VisEntityResumeViewFailed.ENTITY.save(put);
		return json;
	}

}
