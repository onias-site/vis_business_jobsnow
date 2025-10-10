package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.vis.cache.VisBusinessPutSkillsInJson;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisBusinessResumeSendToRecruiters;

public class VisBusinessSendResumeToRecruiters implements CcpBusiness {
	//TODO 

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisBusinessPutSkillsInJson putSkillsInJson = new VisBusinessPutSkillsInJson("resumeText", VisEntityResume.Fields.skill.name());
		
		CcpJsonRepresentation jsonWithSkills = json.extractInformationFromJson(putSkillsInJson);
		
		JnExecuteBulkOperation.INSTANCE.executeSelectUnionAllThenSaveInTheMainAndTwinEntities(
				jsonWithSkills, 
				VisEntityResume.ENTITY, 
				VisBusinessResumeSendToRecruiters.INSTANCE
				);
		
		return jsonWithSkills;
	}

}
