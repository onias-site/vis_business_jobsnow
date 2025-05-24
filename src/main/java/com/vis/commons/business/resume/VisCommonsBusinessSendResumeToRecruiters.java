package com.vis.commons.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.vis.commons.cache.tasks.PutSkillsInJson;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.utils.VisAsyncBusinessResumeSendToRecruiters;

public class VisCommonsBusinessSendResumeToRecruiters implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		PutSkillsInJson putSkillsInJson = new PutSkillsInJson("resumeText", VisEntityResume.Fields.skill.name());
		
		CcpJsonRepresentation jsonWithSkills = json.extractInformationFromJson(putSkillsInJson);
		
		JnExecuteBulkOperation.INSTANCE.executeSelectUnionAllThenSaveInTheMainAndTwinEntities(
				jsonWithSkills, 
				VisEntityResume.ENTITY, 
				VisAsyncBusinessResumeSendToRecruiters.INSTANCE
				);
		
		return jsonWithSkills;
	}

}
