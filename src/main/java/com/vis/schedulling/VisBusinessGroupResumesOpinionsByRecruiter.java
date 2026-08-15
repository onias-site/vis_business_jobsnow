package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityGroupResumesPerceptionsByRecruiter;
import com.vis.entities.VisEntityResumePerception;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Tarefa agendada que agrupa as percepções/avaliações de currículos pelo e-mail do recrutador, usando
 * VisEntityResumePerception como fonte e VisEntityGroupResumesPerceptionsByRecruiter como destino.
 * Delega ao utilitário VisUtils.groupDetailsByMasters.
 */
public class VisBusinessGroupResumesOpinionsByRecruiter implements CcpBusiness{
		

	private VisBusinessGroupResumesOpinionsByRecruiter() {}
	
	public static final VisBusinessGroupResumesOpinionsByRecruiter INSTANCE = new VisBusinessGroupResumesOpinionsByRecruiter();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupDetailsByMasters = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumePerception.ENTITY, 
				VisEntityGroupResumesPerceptionsByRecruiter.ENTITY, 
				VisJsonCommonsFields.recruiter, 
				JnJsonCommonsFields.timestamp
				);
		
		return groupDetailsByMasters;
	}

}
