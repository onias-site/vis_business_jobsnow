package com.vis.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;

/**
 * Disparada após o save ou reativação de um currículo para enviá-lo imediatamente a recrutadores com vagas
 * compatíveis na frequência minute. Usa o currículo recém-salvo como única fonte de currículos e busca todas
 * as vagas ativas da frequência minute.
 */
public class VisBusinessResumeSendToRecruiters implements CcpBusiness {
	
	private VisBusinessResumeSendToRecruiters() {}
	
	public static final VisBusinessResumeSendToRecruiters INSTANCE = new VisBusinessResumeSendToRecruiters();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation resumeWithSkills) {
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes = x -> Arrays.asList(resumeWithSkills);
		
		Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(frequency);
		//TODO TROCAR STATIC POR FUNCOES
		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(VisFrequencyOptions.minute, howToObtainResumes, howToObtainPositionsGroupedByRecruiters);
		
		return resumeWithSkills;
	}
	
}
