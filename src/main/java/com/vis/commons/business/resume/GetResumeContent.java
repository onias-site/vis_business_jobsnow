package com.vis.commons.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntityCrudOperationType;
import com.ccp.exceptions.process.CcpFlowDisturb;
import com.jn.mensageria.JnMensageriaSender;
import com.vis.commons.entities.VisEntityBalance;
import com.vis.commons.entities.VisEntityFees;
import com.vis.commons.entities.VisEntityResumeLastView;
import com.vis.commons.entities.VisEntityResumeViewFailed;
import com.vis.commons.status.ViewResumeStatus;
import com.vis.commons.utils.VisCommonsUtils;

public class GetResumeContent implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	
	private GetResumeContent() {}
	
	public static final GetResumeContent INSTANCE = new GetResumeContent();
	
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
		
		boolean insufficientFunds = VisCommonsUtils.isInsufficientFunds(1, fee, balance);
		
		if(insufficientFunds) {
			CcpJsonRepresentation put = json.put("status", ViewResumeStatus.insufficientFunds);
			VisEntityResumeViewFailed.ENTITY.createOrUpdate(put);
			throw new CcpFlowDisturb(json, ViewResumeStatus.insufficientFunds);
		}
		//TODO EH ESTA ENTIDADE MESMO???
		new JnMensageriaSender(VisEntityResumeLastView.ENTITY, CcpEntityCrudOperationType.save).apply(json);
		//LATER IMPLEMENTAR LOGICA PARA FORMAR NOME DO ARQUIVO DO CURRICULO EM CASO DE SER RECRUTADOR BAIXANDO POR VAGA OU NAO
		CcpJsonRepresentation resume = VisCommonsUtils.getResumeFromBucket(json);
		
		return resume;
	}


}
