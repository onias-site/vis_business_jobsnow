package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Implementação de CcpBusiness que representa o passo de envio de currículos associados a uma vaga.
 * A implementação ainda está pendente (retorna o JSON sem alteração — TODO).
 */
public class VisBusinessPositionResumesSend implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessPositionResumesSend() {}
	
	public static final VisBusinessPositionResumesSend INSTANCE = new VisBusinessPositionResumesSend();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
