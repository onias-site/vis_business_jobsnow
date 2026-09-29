package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.jn.services.JnService;
import com.vis.business.recruiter.VisBusinessRecruiterReceivingResumes;
import com.vis.entities.VisEntityGroupPositionsByRecruiter;
import com.vis.entities.VisEntityGroupResumesPerceptionsByRecruiter;
import com.vis.entities.VisEntityResumePerception;


/**
 * Recruiter data access service. Exposes the operations related to the recruiter's interactions
 * with resumes and positions.
 */
public enum VisServiceRecruiter implements JnService {
	GetAlreadySeenResumes{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityGroupResumesPerceptionsByRecruiter.ENTITY.getOneByIdAnyWhere(json);
			
			return result;
		}
	},
	GetPositionsFromThisRecruiter{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityGroupPositionsByRecruiter.ENTITY.getOneByIdAnyWhere(json);
			
			return result;
		}
	},
	SaveOpinionAboutThisResume{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityResumePerception.ENTITY.save(json);

			return json;
		}
	},
	SendResumesToEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			JnFunctionMensageriaSender mensageriaSender = new JnFunctionMensageriaSender(VisBusinessRecruiterReceivingResumes.INSTANCE);
			CcpJsonRepresentation result = mensageriaSender.execute(json);
			
			return result;
		}
	},
	ChangeOpinionAboutThisResume{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityResumePerception.ENTITY.delete(json);

			return json;
		}
	},
	;





}
