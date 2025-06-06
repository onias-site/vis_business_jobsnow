package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.text.extractor.CcpTextExtractor;
import com.vis.exceptions.VisErrorBusinessEmptyResumeText;

public class VisBusinessExtractTextFromResume implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	private VisBusinessExtractTextFromResume () {}
	
	public static final VisBusinessExtractTextFromResume INSTANCE = new VisBusinessExtractTextFromResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpTextExtractor textExtractor = CcpDependencyInjection.getDependency(CcpTextExtractor.class);

		String resumeBase64 = json.getAsString("resumeBase64");

		String resumeText = textExtractor.extractText(resumeBase64);
		
		boolean emptyText = resumeText.trim().isEmpty();
		
		if(emptyText) {
			throw new VisErrorBusinessEmptyResumeText(json);
		}
		
		CcpJsonRepresentation put = json.put("resumeText", resumeText);
		
		return put;
	}

}
