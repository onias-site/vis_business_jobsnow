package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusinessSendToMensageria;
import com.vis.entities.VisEntityGroupResumeViewsByResume;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Tarefa agendada que agrupa as visualizações de currículos pelo e-mail do candidato (currículo),
 * usando VisEntityResumeFreeView como fonte e VisEntityGroupResumeViewsByResume como destino.
 * Delega a lógica ao utilitário VisUtils.groupDetailsByMasters.
 */
public class VisBusinessGroupResumeViewsByResume implements JnBusinessSendToMensageria{
		

	private VisBusinessGroupResumeViewsByResume() {}
	
	public static final VisBusinessGroupResumeViewsByResume INSTANCE = new VisBusinessGroupResumeViewsByResume();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByResume.ENTITY, 
				VisJsonCommonsFields.email, 
				JnJsonCommonsFields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
