package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityPosition;
public class VisBusinessDuplicateFieldEmailToFieldMasters implements CcpBusiness{
	//TODO JSON VALIDATIONS	
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
