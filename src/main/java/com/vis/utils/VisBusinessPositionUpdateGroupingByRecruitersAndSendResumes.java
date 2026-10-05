package com.vis.utils;

import java.util.List;
import java.util.function.Function;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.vis.entities.VisEntityGroupResumesByPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLastView;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;


/**
 * Orchestrates the complete update of the grouping of positions by recruiters and the sending of matching
 * resumes, when triggered after a position save or delete. Duplicates the email field into masters,
 * regroups the positions by recruiter, fetches every resume of the last year, filters and sorts the resumes
 * that match the position, and saves the paginated result in VisEntityGroupResumesByPosition.
 */
public class VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes implements CcpBusiness{

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes() {}
	
	/** The single instance. */
	public static final VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes INSTANCE = new VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes();
	//0
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation jsonWithMasters = json.duplicateValueFromField(VisJsonCommonsFields.email, VisJsonCommonsFields.masters);

		VisUtils.groupPositionsGroupedByRecruiters(jsonWithMasters);
		
		Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getLastUpdatedResumes = x -> VisUtils.getLastUpdated(VisEntityResume.ENTITY, VisFrequencyOptions.yearly, JnJsonCommonsFields.timestamp.name());
		
		List<String> email = json.getAsStringList(VisJsonCommonsFields.email);

		Function<VisFrequencyOptions, CcpJsonRepresentation> getSavingPosition = frequency -> CcpOtherConstants.EMPTY_JSON.put(new CcpFieldName(email.get(0)), json);

		List<CcpJsonRepresentation> positionsWithFilteredAndSortedResumesAndTheirStatis = VisUtils.sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(json, getLastUpdatedResumes, getSavingPosition);
		
		CcpJsonRepresentation positionWithFilteredAndSortedResumesAndTheirStatis = positionsWithFilteredAndSortedResumesAndTheirStatis.get(0);
		
		List<CcpJsonRepresentation> records = positionWithFilteredAndSortedResumesAndTheirStatis.getAsJsonList(VisJsonCommonsFields.resumes);
		
		CcpJsonRepresentation position = positionWithFilteredAndSortedResumesAndTheirStatis.getInnerJson(VisEntityResumeLastView.Fields.position);

		VisUtils.saveRecordsInPages(records, position, VisEntityGroupResumesByPosition.ENTITY);
		
		return positionWithFilteredAndSortedResumesAndTheirStatis;
	}

}
