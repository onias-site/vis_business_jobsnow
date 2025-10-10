package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
//FIXME EXTRAIR SKILLS NO ENDPOINT DE SALVAMENTO DE CURRICULOS
public class VisBusinessExtractSkillsFromText implements CcpBusiness {
	
	public final static VisBusinessExtractSkillsFromText INSTANCE = new VisBusinessExtractSkillsFromText();
	
	private VisBusinessExtractSkillsFromText() {}
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// FIXME Auto-generated method stub
		return json;
	}

}
