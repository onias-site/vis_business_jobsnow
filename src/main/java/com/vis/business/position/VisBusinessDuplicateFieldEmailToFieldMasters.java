package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.vis.entities.VisEntityPosition;
enum VisBusinessDuplicateFieldEmailToFieldMastersConstants  implements CcpJsonFieldName{
	masters
	
}
public class VisBusinessDuplicateFieldEmailToFieldMasters implements CcpTopic{

	public static final VisBusinessDuplicateFieldEmailToFieldMasters INSTANCE = new VisBusinessDuplicateFieldEmailToFieldMasters();
	
	private VisBusinessDuplicateFieldEmailToFieldMasters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation duplicateValueFromField = json.duplicateValueFromField(VisEntityPosition.Fields.email, VisBusinessDuplicateFieldEmailToFieldMastersConstants.masters);
		return duplicateValueFromField;
	}

}
