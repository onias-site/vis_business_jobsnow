package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.ccp.especifications.text.extractor.CcpTextExtractor;
import com.vis.exceptions.VisErrorBusinessEmptyResumeText;

public class VisBusinessExtractTextFromResume implements CcpBusiness {
	enum JsonFieldNames implements CcpJsonFieldName{
		resumeBase64, resumeText
	}
	private VisBusinessExtractTextFromResume () {}
	
	public static final VisBusinessExtractTextFromResume INSTANCE = new VisBusinessExtractTextFromResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpTextExtractor textExtractor = CcpDependencyInjection.getDependency(CcpTextExtractor.class);

		String resumeBase64 = json.getAsString(JsonFieldNames.resumeBase64);

		String resumeText = textExtractor.extractText(resumeBase64);
		
		boolean emptyText = resumeText.trim().isEmpty();
		
		if(emptyText) {
			throw new VisErrorBusinessEmptyResumeText(json);
		}
		
		CcpJsonRepresentation put = json.put(JsonFieldNames.resumeText, resumeText);
		
		return put;
	}

}
