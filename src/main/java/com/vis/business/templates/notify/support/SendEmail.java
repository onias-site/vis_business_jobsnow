package com.vis.business.templates.notify.support;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;

class SendEmail implements CcpBusiness{

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		var clazz = this.getClass();
		String simpleName = clazz.getSimpleName();
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(simpleName);
		CcpTextDecorator ccpStringDecoratorText = ccpStringDecorator.text();
		var toSnakeCase = ccpStringDecoratorText.toSnakeCase();
		String lowerCase = toSnakeCase.content.toLowerCase();
		VisTemplatesToNotifySupport valueOf = VisTemplatesToNotifySupport.valueOf(lowerCase);
		CcpJsonRepresentation apply = valueOf.execute(json);
		return apply;
	}
	
}
