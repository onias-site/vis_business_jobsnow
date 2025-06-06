package com.vis.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;

public class VisBusinessResumeSendToRecruiters implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	
	private VisBusinessResumeSendToRecruiters() {}
	
	public static final VisBusinessResumeSendToRecruiters INSTANCE = new VisBusinessResumeSendToRecruiters();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation resumeWithSkills) {
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes = x -> Arrays.asList(resumeWithSkills);
		
		Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(frequency);
		
		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(VisFrequencyOptions.minute, howToObtainResumes, howToObtainPositionsGroupedByRecruiters);
		
		return resumeWithSkills;
	}
	
}
