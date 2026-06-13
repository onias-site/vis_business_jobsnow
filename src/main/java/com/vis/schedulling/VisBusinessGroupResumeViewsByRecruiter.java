package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusinessSendToMensageria;
import com.vis.entities.VisEntityGroupResumeViewsByRecruiter;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisUtils;

/**
 * Tarefa agendada que agrupa as visualizações de currículos por recrutador, usando VisEntityResumeFreeView
 * como fonte de dados e VisEntityGroupResumeViewsByRecruiter como destino do agrupamento paginado.
 * Delega a lógica ao utilitário VisUtils.groupDetailsByMasters.
 */
public class VisBusinessGroupResumeViewsByRecruiter implements JnBusinessSendToMensageria{
	//TODO JSON VALIDATIONS	

	private VisBusinessGroupResumeViewsByRecruiter() {}
	
	public static final VisBusinessGroupResumeViewsByRecruiter INSTANCE = new VisBusinessGroupResumeViewsByRecruiter();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByRecruiter.ENTITY, 
				VisEntityResumeFreeView.Fields.email, 
				VisEntityResumeFreeView.Fields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
