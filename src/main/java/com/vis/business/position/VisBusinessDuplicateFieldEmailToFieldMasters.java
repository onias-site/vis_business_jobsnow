package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityPosition;
/**
 * Implementação de CcpBusiness que copia o valor do campo email da entidade VisEntityPosition
 * para o campo masters no mesmo JSON. Utilizada como etapa preparatória para operações de
 * agrupamento que requerem o campo masters populado.
 */
public class VisBusinessDuplicateFieldEmailToFieldMasters implements CcpBusiness{
		
	enum JsonFieldNames implements CcpJsonFieldName{
		masters
	}

	public static final VisBusinessDuplicateFieldEmailToFieldMasters INSTANCE = new VisBusinessDuplicateFieldEmailToFieldMasters();
	
	private VisBusinessDuplicateFieldEmailToFieldMasters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation duplicateValueFromField = json.duplicateValueFromField(VisEntityPosition.Fields.email, JsonFieldNames.masters);
		return duplicateValueFromField;
	}

}
