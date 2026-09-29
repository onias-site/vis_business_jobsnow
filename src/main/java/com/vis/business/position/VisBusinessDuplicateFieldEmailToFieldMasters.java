package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.json.fields.validation.VisJsonCommonsFields;


/**
 * CcpBusiness implementation that copies the value of the email field of the VisEntityPosition entity
 * into the masters field of the same JSON. Used as a preparatory step for grouping operations
 * that require the masters field to be filled.
 */
public class VisBusinessDuplicateFieldEmailToFieldMasters implements CcpBusiness{
		

	public static final VisBusinessDuplicateFieldEmailToFieldMasters INSTANCE = new VisBusinessDuplicateFieldEmailToFieldMasters();
	
	private VisBusinessDuplicateFieldEmailToFieldMasters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation jsonWithMasters = json.duplicateValueFromField(VisJsonCommonsFields.email, VisJsonCommonsFields.masters);
		return jsonWithMasters;
	}

}
