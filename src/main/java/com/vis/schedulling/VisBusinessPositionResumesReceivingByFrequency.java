package com.vis.schedulling;

import java.util.List;
import java.util.function.Function;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisFrequencyOptions;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Main scheduled task of the matching process. For a given sending frequency, it fetches the positions
 * grouped by recruiter and the recently updated resumes, and orchestrates the filtering, sorting and
 * sending of the matching resumes to each recruiter. It is the entry point of the periodic matching
 * cycle (minute, hourly, daily, weekly, monthly).
 */
public class VisBusinessPositionResumesReceivingByFrequency  implements CcpBusiness{
		

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessPositionResumesReceivingByFrequency() {}
	
	/** The single instance. */
	public static final VisBusinessPositionResumesReceivingByFrequency INSTANCE = new VisBusinessPositionResumesReceivingByFrequency();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param schedullingPlan the scheduling plan, with the {@code frequency}
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation schedullingPlan) {

		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisUtils.getLastUpdated(VisEntityResume.ENTITY, VisFrequencyOptions.valueOf(x.getAsString(VisEntityPosition.Fields.frequency)), JnJsonCommonsFields.timestamp.name());

		Function<VisFrequencyOptions, CcpJsonRepresentation> getLastUpdatedPositions = frequency -> VisUtils.getAllPositionsGroupedByRecruiters(frequency);

		VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, getLastUpdatedResumes, getLastUpdatedPositions);
	
		return schedullingPlan;
	}
}
