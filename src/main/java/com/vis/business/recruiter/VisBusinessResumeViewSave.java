package com.vis.business.recruiter;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.business.CcpBusiness;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.entities.VisEntityResumeLastView;
import com.vis.entities.VisEntityResumePerception;
import com.ccp.especifications.db.utils.entity.CcpEntity;

/**
 * CcpBusiness implementation that records a recruiter viewing a resume.
 * Checks whether the view is free or paid (the financial part is pending), whether the resume is
 * negativated and whether the position is inactive, and then persists the VisEntityResumeLastView
 * and VisEntityResumeFreeView records in a bulk operation.
 */
public class VisBusinessResumeViewSave implements CcpBusiness{
		

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessResumeViewSave() {}
	
	/** The single instance. */
	public static final VisBusinessResumeViewSave INSTANCE = new VisBusinessResumeViewSave();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		boolean resumeViewIsNotFree = VisEntityResumeFreeView.ENTITY.exists(json);
		
		if(resumeViewIsNotFree) {
			//LATER IMPLEMENT THE FINANCIAL PART
		}
		CcpEntity resumePerceptionTwinEntity = VisEntityResumePerception.ENTITY.getTwinEntity();

		boolean negativatedResume = resumePerceptionTwinEntity.exists(json);
		CcpEntity positionTwinEntity = VisEntityPosition.ENTITY.getTwinEntity();
		boolean inactivePosition = positionTwinEntity.exists(json);
	
//		CcpJsonRepresentation opinion = VisEntityResumePerception.INSTANCE.getInnerJsonFromMainAndMirrorEntities(json);
		CcpJsonRepresentation position = VisEntityPosition.ENTITY.getOneById(json);
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getOneById(json);
		CcpJsonRepresentation jsonWithResume = json
				.put(VisEntityResumeLastView.Fields.resume, resume);
				CcpJsonRepresentation jsonWithPosition = jsonWithResume
//				.put(VisEntityResumeLastView.Fields.opinion.name(), opinion)
				.put(VisEntityResumeLastView.Fields.position, position);
				CcpJsonRepresentation jsonWithInactivePosition = jsonWithPosition
				.put(VisEntityResumeLastView.Fields.inactivePosition, inactivePosition);

				CcpJsonRepresentation dataToSave = jsonWithInactivePosition
				.put(VisEntityResumeLastView.Fields.negativatedResume, negativatedResume)
				;
		
		var itemResumeLastView = VisEntityResumeLastView.ENTITY.toBulkItems(dataToSave, CcpBulkEntityOperationType.create);
		var itemResumeFreeView = VisEntityResumeFreeView.ENTITY.toBulkItems(dataToSave, CcpBulkEntityOperationType.create);
		List<CcpBulkItem> bulkItems = new ArrayList<>();
	
		bulkItems.addAll(itemResumeFreeView);
		bulkItems.addAll(itemResumeLastView);
		
		JnExecuteBulkOperation.INSTANCE.executeBulk(bulkItems, array -> {});
		return json;
	}

	
}
