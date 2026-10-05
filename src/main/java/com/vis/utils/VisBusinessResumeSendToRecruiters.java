package com.vis.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Triggered after a resume is saved or reactivated, to send it right away to the recruiters with matching
 * positions in the minute frequency. Uses the freshly saved resume as the only source of resumes and fetches
 * every active position of the minute frequency.
 */
public class VisBusinessResumeSendToRecruiters implements CcpBusiness {
	
	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessResumeSendToRecruiters() {}
	
	/** The single instance. */
	public static final VisBusinessResumeSendToRecruiters INSTANCE = new VisBusinessResumeSendToRecruiters();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param resumeWithSkills the resume just saved
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation resumeWithSkills) {
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes = x -> Arrays.asList(resumeWithSkills);
		
		Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(frequency);
		//TODO REPLACE STATIC WITH FUNCTIONS
		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(VisFrequencyOptions.minute, howToObtainResumes, howToObtainPositionsGroupedByRecruiters);
		
		return resumeWithSkills;
	}
	
}
