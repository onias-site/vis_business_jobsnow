package com.vis.utils;

import java.util.List;
import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityGroupResumesByPosition;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLastView;

public class VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes implements  Function<CcpJsonRepresentation, CcpJsonRepresentation>{

	private VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes() {}
	
	public static final VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes INSTANCE = new VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes();
	//0
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation duplicateValueFromKey = json.duplicateValueFromField(VisEntityPosition.Fields.email.name(), "masters");

		VisUtils.groupPositionsGroupedByRecruiters(duplicateValueFromKey);
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisUtils.getLastUpdated(VisEntityResume.ENTITY, VisFrequencyOptions.yearly, VisEntityResume.Fields.timestamp.name());
		
		List<String> email = json.getAsStringList(VisEntityPosition.Fields.email.name());

		Function<VisFrequencyOptions, CcpJsonRepresentation> getSavingPosition = frequency -> CcpOtherConstants.EMPTY_JSON.put(email.get(0), json);

		List<CcpJsonRepresentation> positionsWithFilteredAndSortedResumesAndTheirStatis = VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(json, getLastUpdatedResumes, getSavingPosition);
		
		CcpJsonRepresentation positionWithFilteredAndSortedResumesAndTheirStatis = positionsWithFilteredAndSortedResumesAndTheirStatis.get(0);
		
		List<CcpJsonRepresentation> records = positionWithFilteredAndSortedResumesAndTheirStatis.getAsJsonList("resumes");
		
		CcpJsonRepresentation position = positionWithFilteredAndSortedResumesAndTheirStatis.getInnerJson(VisEntityResumeLastView.Fields.position.name());

		VisUtils.saveRecordsInPages(records, position, VisEntityGroupResumesByPosition.ENTITY);
		
		//FORGOT descobrir uma forma de gravar o agrupamento de vagas por currículos
		
		return positionWithFilteredAndSortedResumesAndTheirStatis;
	}

}
