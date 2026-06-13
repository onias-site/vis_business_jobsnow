package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Tarefa agendada destinada ao agrupamento de skills. Implementação ainda pendente — retorna o JSON
 * de entrada sem processamento.
 */
public class VisBusinessGroupSkills implements  CcpBusiness{

	private VisBusinessGroupSkills() {}
	
	public static final VisBusinessGroupSkills INSTANCE = new VisBusinessGroupSkills();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		return json;
	}

}
