package com.vis.schedulling;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisFrequencyOptions;
import com.vis.utils.VisAsyncUtils;

public class VisBusinessPositionResumesReceivingByFrequency  implements CcpTopic{

	private VisBusinessPositionResumesReceivingByFrequency() {}
	
	public static final VisBusinessPositionResumesReceivingByFrequency INSTANCE = new VisBusinessPositionResumesReceivingByFrequency();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation schedullingPlan) {

		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisAsyncUtils.getLastUpdated(VisEntityResume.ENTITY, VisFrequencyOptions.valueOf(x.getAsString(VisEntityPosition.Fields.frequency.name())), VisEntityPosition.Fields.timestamp.name());

		Function<String, CcpJsonRepresentation> getLastUpdatedPositions = frequency -> VisAsyncUtils.getAllPositionsGroupedByRecruiters(VisFrequencyOptions.valueOf(frequency));

		VisAsyncUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, getLastUpdatedResumes, getLastUpdatedPositions);
	
		return schedullingPlan;
	}
}
