package com.vis.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;

public class VisBusinessResumeSendToRecruiters implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	
	private VisBusinessResumeSendToRecruiters() {}
	
	public static final VisBusinessResumeSendToRecruiters INSTANCE = new VisBusinessResumeSendToRecruiters();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation resumeWithSkills) {
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getSavingResume = x -> Arrays.asList(resumeWithSkills);
		
		Function<String, CcpJsonRepresentation> getLastUpdatedPositions = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(VisFrequencyOptions.valueOf(frequency));
		
		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(
				CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPosition.Fields.frequency.name(), VisFrequencyOptions.minute), getSavingResume, getLastUpdatedPositions);
		
		return resumeWithSkills;
	}
	
}
