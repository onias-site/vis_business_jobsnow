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

public class VisBusinessResumeViewSave implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessResumeViewSave() {}
	
	public static final VisBusinessResumeViewSave INSTANCE = new VisBusinessResumeViewSave();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		boolean resumeViewIsNotFree = VisEntityResumeFreeView.ENTITY.isPresentInThisJsonInMainEntity(json);
		
		if(resumeViewIsNotFree) {
			//LATER IMPLEMENTAR PARTE FINANCEIRA
		}

		boolean negativatedResume = VisEntityResumePerception.ENTITY.getTwinEntity().isPresentInThisJsonInMainEntity(json);
		boolean inactivePosition = VisEntityPosition.ENTITY.getTwinEntity().isPresentInThisJsonInMainEntity(json);
	
//		CcpJsonRepresentation opinion = VisEntityResumePerception.INSTANCE.getInnerJsonFromMainAndMirrorEntities(json);
		CcpJsonRepresentation position = VisEntityPosition.ENTITY.getInnerJsonFromMainAndTwinEntities(json);
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getInnerJsonFromMainAndTwinEntities(json);
		
		CcpJsonRepresentation dataToSave = json
				.put(VisEntityResumeLastView.Fields.resume, resume)
//				.put(VisEntityResumeLastView.Fields.opinion.name(), opinion)
				.put(VisEntityResumeLastView.Fields.position, position)
				.put(VisEntityResumeLastView.Fields.inactivePosition, inactivePosition)
				.put(VisEntityResumeLastView.Fields.negativatedResume, negativatedResume)
				;
		
		var itemResumeLastView = VisEntityResumeLastView.ENTITY.toBulkItems(dataToSave, CcpBulkEntityOperationType.create);
		var itemResumeFreeView = VisEntityResumeFreeView.ENTITY.toBulkItems(dataToSave, CcpBulkEntityOperationType.create);
		List<CcpBulkItem> bulkItems = new ArrayList<>();
	
		bulkItems.addAll(itemResumeFreeView);
		bulkItems.addAll(itemResumeLastView);
		
		JnExecuteBulkOperation.INSTANCE.executeBulk(bulkItems);
		return json;
	}

	
}
