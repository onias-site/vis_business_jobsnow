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
 * Implementação de CcpBusiness que registra a visualização de um currículo por um recrutador.
 * Verifica se a visualização é gratuita ou paga (parte financeira pendente), se o currículo está
 * negativado e se a vaga está inativa, e então persiste os registros de VisEntityResumeLastView
 * e VisEntityResumeFreeView em operação bulk.
 */
public class VisBusinessResumeViewSave implements CcpBusiness{
		

	private VisBusinessResumeViewSave() {}
	
	public static final VisBusinessResumeViewSave INSTANCE = new VisBusinessResumeViewSave();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		boolean resumeViewIsNotFree = VisEntityResumeFreeView.ENTITY.exists(json);
		
		if(resumeViewIsNotFree) {
			//LATER IMPLEMENTAR PARTE FINANCEIRA
		}
		CcpEntity twinEntity = VisEntityResumePerception.ENTITY.getTwinEntity();

		boolean negativatedResume = twinEntity.exists(json);
		CcpEntity twinEntity2 = VisEntityPosition.ENTITY.getTwinEntity();
		boolean inactivePosition = twinEntity2.exists(json);
	
//		CcpJsonRepresentation opinion = VisEntityResumePerception.INSTANCE.getInnerJsonFromMainAndMirrorEntities(json);
		CcpJsonRepresentation position = VisEntityPosition.ENTITY.getOneById(json);
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getOneById(json);
		CcpJsonRepresentation put = json
				.put(VisEntityResumeLastView.Fields.resume, resume);
				CcpJsonRepresentation put2 = put
//				.put(VisEntityResumeLastView.Fields.opinion.name(), opinion)
				.put(VisEntityResumeLastView.Fields.position, position);
				CcpJsonRepresentation put3 = put2
				.put(VisEntityResumeLastView.Fields.inactivePosition, inactivePosition);

				CcpJsonRepresentation dataToSave = put3
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
