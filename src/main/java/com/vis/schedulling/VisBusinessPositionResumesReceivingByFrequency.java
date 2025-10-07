package com.vis.schedulling;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisFrequencyOptions;
import com.vis.utils.VisUtils;

public class VisBusinessPositionResumesReceivingByFrequency  implements CcpTopic{
	//TODO JSON VALIDATIONS	

	private VisBusinessPositionResumesReceivingByFrequency() {}
	
	public static final VisBusinessPositionResumesReceivingByFrequency INSTANCE = new VisBusinessPositionResumesReceivingByFrequency();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation schedullingPlan) {

		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisUtils.getLastUpdated(VisEntityResume.ENTITY, VisFrequencyOptions.valueOf(x.getAsString(VisEntityPosition.Fields.frequency)), VisEntityPosition.Fields.timestamp.name());

		Function<VisFrequencyOptions, CcpJsonRepresentation> getLastUpdatedPositions = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(frequency);

		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, getLastUpdatedResumes, getLastUpdatedPositions);
	
		return schedullingPlan;
	}
}
