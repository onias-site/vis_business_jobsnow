package com.vis.commons.utils;

import java.util.List;
import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.commons.entities.VisEntityGroupResumesByPosition;
import com.vis.commons.entities.VisEntityPosition;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.entities.VisEntityResumeLastView;

public class VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes implements  Function<CcpJsonRepresentation, CcpJsonRepresentation>{

	private VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes() {}
	
	public static final VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes INSTANCE = new VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes();
	//0
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation duplicateValueFromKey = json.duplicateValueFromField(VisEntityPosition.Fields.email.name(), "masters");

		VisAsyncUtils.groupPositionsGroupedByRecruiters(duplicateValueFromKey);
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisAsyncUtils.getLastUpdated(VisEntityResume.ENTITY, FrequencyOptions.yearly, VisEntityResume.Fields.timestamp.name());
		
		List<String> email = json.getAsStringList(VisEntityPosition.Fields.email.name());

		Function<String, CcpJsonRepresentation> getSavingPosition = frequency -> CcpOtherConstants.EMPTY_JSON.put(email.get(0), json);

		List<CcpJsonRepresentation> positionsWithFilteredAndSortedResumesAndTheirStatis = VisAsyncUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(json, getLastUpdatedResumes, getSavingPosition);
		
		CcpJsonRepresentation positionWithFilteredAndSortedResumesAndTheirStatis = positionsWithFilteredAndSortedResumesAndTheirStatis.get(0);
		
		List<CcpJsonRepresentation> records = positionWithFilteredAndSortedResumesAndTheirStatis.getAsJsonList("resumes");
		
		CcpJsonRepresentation position = positionWithFilteredAndSortedResumesAndTheirStatis.getInnerJson(VisEntityResumeLastView.Fields.position.name());

		VisAsyncUtils.saveRecordsInPages(records, position, VisEntityGroupResumesByPosition.ENTITY);
		
		//FORGOT descobrir uma forma de gravar o agrupamento de vagas por currículos
		
		return positionWithFilteredAndSortedResumesAndTheirStatis;
	}

}
