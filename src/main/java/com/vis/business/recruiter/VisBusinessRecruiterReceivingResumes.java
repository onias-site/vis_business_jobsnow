package com.vis.business.recruiter;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Implementação de CcpBusiness que representa o processo de recebimento de currículos pelo recrutador.
 * A lógica está pendente de implementação (retorna o JSON de entrada — TODO).
 */
public class VisBusinessRecruiterReceivingResumes implements CcpBusiness{
		

	private VisBusinessRecruiterReceivingResumes() {}
	
	public static final VisBusinessRecruiterReceivingResumes INSTANCE = new VisBusinessRecruiterReceivingResumes();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
