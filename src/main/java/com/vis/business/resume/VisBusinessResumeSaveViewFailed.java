package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityResumeViewFailed;
import com.jn.json.fields.validation.JnJsonCommonsFields;

import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * Implementação de CcpBusiness que persiste o registro de uma tentativa de visualização de currículo
 * que falhou. Extrai o status HTTP do campo errorDetails.status e salva o registro na entidade
 * VisEntityResumeViewFailed.
 */
public class VisBusinessResumeSaveViewFailed implements CcpBusiness {

	private VisBusinessResumeSaveViewFailed() {}
	
	public static final VisBusinessResumeSaveViewFailed INSTANCE = new VisBusinessResumeSaveViewFailed();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String status = json.getValueFromPath("", CcpJsonCommonsFields.errorDetails, JnJsonCommonsFields.status);
		CcpJsonRepresentation put = json.put(JnJsonCommonsFields.status, status);
		VisEntityResumeViewFailed.ENTITY.save(put);
		return json;
	}

}
