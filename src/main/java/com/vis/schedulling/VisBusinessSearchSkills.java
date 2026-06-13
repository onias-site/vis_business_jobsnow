package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Tarefa agendada destinada à busca de skills. Implementação pendente — retorna o JSON de entrada
 * sem processamento.
 */
public class VisBusinessSearchSkills implements  CcpBusiness{

	private VisBusinessSearchSkills() {}
	
	public static final VisBusinessSearchSkills INSTANCE = new VisBusinessSearchSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
