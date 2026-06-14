package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.utils.VisUtils;

/**
 * Implementação de CcpBusiness que delega ao utilitário VisUtils.groupPositionsGroupedByRecruiters
 * o agrupamento das vagas por recrutador. Serve como ponto de entrada de negócio para disparar esse agrupamento.
 */
public class VisBusinessGroupPositionsGroupedByRecruiters implements CcpBusiness{
		

	public static final VisBusinessGroupPositionsGroupedByRecruiters INSTANCE = new VisBusinessGroupPositionsGroupedByRecruiters();
	
	private VisBusinessGroupPositionsGroupedByRecruiters() {}
	
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupPositionsGroupedByRecruiters = VisUtils.groupPositionsGroupedByRecruiters(json);
		return groupPositionsGroupedByRecruiters;
	}

}
