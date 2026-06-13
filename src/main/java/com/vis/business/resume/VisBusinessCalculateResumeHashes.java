package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;

//TODO
/**
 * Implementação de CcpBusiness destinada ao cálculo de hashes do currículo (para indexação ou matching).
 * A implementação está marcada como TODO — retorna o JSON de entrada sem alterações.
 */
public class VisBusinessCalculateResumeHashes implements CcpBusiness {
	
	public static enum JsonFieldNames implements CcpJsonFieldName{
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		return json;
	}
	
	public Class<?> getJsonValidationClass() {
		return JsonFieldNames.class;
	}

}
