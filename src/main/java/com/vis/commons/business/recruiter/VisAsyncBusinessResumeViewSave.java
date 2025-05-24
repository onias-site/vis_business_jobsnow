package com.vis.commons.business.recruiter;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpEntityBulkOperationType;
import com.ccp.especifications.mensageria.receiver.CcpTopic;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.vis.commons.entities.VisEntityPosition;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.entities.VisEntityResumeFreeView;
import com.vis.commons.entities.VisEntityResumeLastView;
import com.vis.commons.entities.VisEntityResumePerception;

public class VisAsyncBusinessResumeViewSave implements CcpTopic{

	private VisAsyncBusinessResumeViewSave() {}
	
	public static final VisAsyncBusinessResumeViewSave INSTANCE = new VisAsyncBusinessResumeViewSave();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		List<CcpBulkItem> bulkItems = new ArrayList<>();
		
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
				.put(VisEntityResumeLastView.Fields.resume.name(), resume)
//				.put(VisEntityResumeLastView.Fields.opinion.name(), opinion)
				.put(VisEntityResumeLastView.Fields.position.name(), position)
				.put(VisEntityResumeLastView.Fields.inactivePosition.name(), inactivePosition)
				.put(VisEntityResumeLastView.Fields.negativatedResume.name(), negativatedResume)
				;
		
		CcpBulkItem itemResumeLastView = VisEntityResumeLastView.ENTITY.getMainBulkItem(dataToSave, CcpEntityBulkOperationType.create);
		CcpBulkItem itemResumeFreeView = VisEntityResumeFreeView.ENTITY.getMainBulkItem(dataToSave, CcpEntityBulkOperationType.create);
		
		bulkItems.add(itemResumeFreeView);
		bulkItems.add(itemResumeLastView);
		
		JnExecuteBulkOperation.INSTANCE.executeBulk(bulkItems);
		return json;
	}

	
}
