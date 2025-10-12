package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.CcpEntityCrudOperationType;
import com.ccp.business.CcpBusiness;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.vis.entities.VisEntityBalance;
import com.vis.entities.VisEntityFees;
import com.vis.entities.VisEntityResumeLastView;
import com.vis.entities.VisEntityResumeViewFailed;
import com.vis.status.VisProcessStatusResumeView;
import com.vis.utils.VisUtils;

public class VisBusinessGetResumeContent implements CcpBusiness{
	enum JsonFieldNames implements CcpJsonFieldName{status}
	
	private VisBusinessGetResumeContent() {}
	
	public static final VisBusinessGetResumeContent INSTANCE = new VisBusinessGetResumeContent();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		/*
		 * VisEntityResume
		 * VisEntityFees
		 * VisEntityResumeView
		 * VisEntityResumeOpinion.INSTANCE.getMirrorEntity()
		 * VisEntityPosition
		 */
		CcpJsonRepresentation balance =  VisEntityBalance.ENTITY.getInnerJsonFromMainAndTwinEntities(json);
		CcpJsonRepresentation fee =  VisEntityFees.ENTITY.getInnerJsonFromMainAndTwinEntities(json);
		
		boolean insufficientFunds = VisUtils.isInsufficientFunds(1, fee, balance);
		
		if(insufficientFunds) {
			CcpJsonRepresentation put = json.put(JsonFieldNames.status, VisProcessStatusResumeView.insufficientFunds);
			VisEntityResumeViewFailed.ENTITY.createOrUpdate(put);
			throw new CcpErrorFlowDisturb(json, VisProcessStatusResumeView.insufficientFunds);
		}
		//ATTENTION EH ESTA ENTIDADE MESMO???
		new JnFunctionMensageriaSender(VisEntityResumeLastView.ENTITY, CcpEntityCrudOperationType.save).apply(json);
		//LATER IMPLEMENTAR LOGICA PARA FORMAR NOME DO ARQUIVO DO CURRICULO EM CASO DE SER RECRUTADOR BAIXANDO POR VAGA OU NAO
		CcpJsonRepresentation resume = VisUtils.getResumeFromBucket(json);
		
		return resume;
	}


}
