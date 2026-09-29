package com.vis.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.ccp.decorators.CcpCollectionDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Comparator of resumes against a specific position. Sorts the resumes by the sorting criteria defined
 * in the position (sortFields), always including desiredSkill (the number of desired skills the resume has)
 * as the final criterion, in descending order.
 */
public class VisSorterResumesByPosition implements Comparator<CcpJsonRepresentation>{

	private final CcpJsonRepresentation position;

	public VisSorterResumesByPosition(CcpJsonRepresentation position) {
		this.position = position;
	}

	public int compare(CcpJsonRepresentation o1, CcpJsonRepresentation o2) {
		
		List<String> desiredSkill = this.position.getAsStringList(VisEntityPosition.Fields.desiredSkill);
		
		CcpJsonRepresentation firstResumeWithDesiredSkills = this.putDesiredSkills(o1, desiredSkill);
		CcpJsonRepresentation secondResumeWithDesiredSkills = this.putDesiredSkills(o2, desiredSkill);
		
		List<String> positionSortFields = this.position.getAsStringList(VisEntityPosition.Fields.sortFields);
		List<String> sortFields = new ArrayList<>(positionSortFields);
		
		String desiredSkillEnumName = ResumeSortOptions.desiredSkill.name();
		boolean desiredSkillChosen = sortFields.contains(desiredSkillEnumName);
		boolean desiredSkillNotChoosed = false == desiredSkillChosen;
		
		if(desiredSkillNotChoosed ) {
			sortFields.add(desiredSkillEnumName);
		}
		
		for (String sortField : sortFields) {
			
			ResumeSortOptions sortOption = ResumeSortOptions.valueOf(sortField);
			int comparationResult = sortOption.compare(firstResumeWithDesiredSkills, secondResumeWithDesiredSkills);
			
			boolean areEquals = comparationResult == 0;
			
			if(areEquals) {
				continue;
			}
			
			return comparationResult;
		}
		return 0;
	}

	private CcpJsonRepresentation putDesiredSkills(CcpJsonRepresentation o1, List<String> desiredSkills) {
		String skillName = VisJsonCommonsFields.skill.name();
		CcpCollectionDecorator resumeSkills = o1.getAsCollectionDecorator(skillName);
		int desiredSkillsCount = resumeSkills.getIntersectList(desiredSkills).size();
		int negativeDesiredSkillsCount = -desiredSkillsCount;
		CcpJsonRepresentation resumeWithDesiredSkillsCount = o1.put(VisEntityPosition.Fields.desiredSkill, negativeDesiredSkillsCount);
		return resumeWithDesiredSkillsCount;
	}

	
	
}
