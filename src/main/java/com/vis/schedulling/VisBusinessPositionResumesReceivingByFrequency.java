package com.vis.schedulling;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisFrequencyOptions;
import com.vis.utils.VisUtils;

/**
 * Tarefa agendada principal do processo de matching. Para uma determinada frequência de envio, busca
 * as vagas agrupadas por recrutador e os currículos atualizados recentemente, e orquestra o filtro,
 * ordenação e envio dos currículos compatíveis para cada recrutador. É a entrada do ciclo de matching
 * periódico (minute, hourly, daily, weekly, monthly).
 */
public class VisBusinessPositionResumesReceivingByFrequency  implements CcpBusiness{
		

	private VisBusinessPositionResumesReceivingByFrequency() {}
	
	public static final VisBusinessPositionResumesReceivingByFrequency INSTANCE = new VisBusinessPositionResumesReceivingByFrequency();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation schedullingPlan) {

		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisUtils.getLastUpdated(VisEntityResume.ENTITY, VisFrequencyOptions.valueOf(x.getAsString(VisEntityPosition.Fields.frequency)), VisEntityPosition.Fields.timestamp.name());

		Function<VisFrequencyOptions, CcpJsonRepresentation> getLastUpdatedPositions = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(frequency);

		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, getLastUpdatedResumes, getLastUpdatedPositions);
	
		return schedullingPlan;
	}
}
