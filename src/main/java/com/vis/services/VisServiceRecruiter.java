package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.recruiter.VisBusinessRecruiterReceivingResumes;
import com.vis.entities.VisEntityGroupPositionsByRecruiter;
import com.vis.entities.VisEntityGroupResumesPerceptionsByRecruiter;
import com.vis.entities.VisEntityResumePerception;

public enum VisServiceRecruiter implements JnService {
	GetAlreadySeenResumes{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityGroupResumesPerceptionsByRecruiter.ENTITY.getData(json, JnDeleteKeysFromCache.INSTANCE);
			
			return result;
		}
	},
	GetPositionsFromThisRecruiter{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityGroupPositionsByRecruiter.ENTITY.getData(json, JnDeleteKeysFromCache.INSTANCE);
			
			return result;
		}
	},
	SaveOpinionAboutThisResume{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityResumePerception.ENTITY.save(json);
			
			return result;
		}
	},
	SendResumesToEmail{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = new JnFunctionMensageriaSender(VisBusinessRecruiterReceivingResumes.INSTANCE).apply(json);
			
			return result;
		}
	},
	ChangeOpinionAboutThisResume{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityResumePerception.ENTITY.transferToReverseEntity(json);
			
			return result;
		}
	},
	;
	enum JsonFieldNames implements CcpJsonFieldName{
		activePosition
	}
	enum GetAlreadySeenResumes{
		
	}
	enum GetPositionsFromThisRecruiter{
		
	}
	enum SaveOpinionAboutThisResume{
		
	}
	enum SendResumesToEmail{
		
	}
	enum ChangeOpinionAboutThisResume{
		
	}
}
