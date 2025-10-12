package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.vis.cache.VisBusinessPutSkillsInJson;
import com.vis.entities.VisEntityPosition;
public class VisBusinessExtractSkillsFromPositionText implements CcpBusiness {
	
	public final static VisBusinessExtractSkillsFromPositionText INSTANCE = new VisBusinessExtractSkillsFromPositionText();
	
	private VisBusinessExtractSkillsFromPositionText() {}
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		VisBusinessPutSkillsInJson positionWithSkills = new VisBusinessPutSkillsInJson(VisEntityPosition.Fields.description, VisEntityPosition.Fields.requiredSkill);

		CcpJsonRepresentation transformedJson = json.getTransformedJson(positionWithSkills);
	
		return transformedJson;
	}

}
