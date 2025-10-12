package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.cache.VisBusinessPutSkillsInJson;
import com.vis.entities.VisEntityResume;
public class VisBusinessExtractSkillsFromText implements CcpBusiness {
	
	public final static VisBusinessExtractSkillsFromText INSTANCE = new VisBusinessExtractSkillsFromText();
	
	private VisBusinessExtractSkillsFromText() {}
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		VisBusinessPutSkillsInJson resumeWithSkills = new VisBusinessPutSkillsInJson(VisBusinessSaveResumeInBucket.JsonFieldNames.resumeText, VisEntityResume.Fields.skill);

		CcpJsonRepresentation transformedJson = json.getTransformedJson(resumeWithSkills);
	
		return transformedJson;
	}

}
