package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;

public enum VisServiceResume implements JnService {
	ChangeStatus{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityResume.ENTITY.transferToReverseEntity(json);

			return  result;
		}
	},
	Delete{
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			CcpJsonRepresentation result = VisEntityResume.ENTITY.delete(sessionValues);

			return result;
		}
	},
	GetData{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation changeStatus = VisEntityResume.ENTITY.getData(json, JnDeleteKeysFromCache.INSTANCE);
			
			return changeStatus;
		}
	},
	Save{
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			
			CcpJsonRepresentation result = VisEntityResume.ENTITY.save(sessionValues);

			return result;
		}
		
		public Class<?> getJsonValidationClass() {
			return VisEntityResume.Fields.class;
		}
	}, 
	;
}
	enum JsonFieldNames implements CcpJsonFieldName{
		activePosition
	}
	enum ChangeStatus{
		
	}
	enum Delete{
		
	}
	enum GetData{
		
	}
	enum GetFile{
		
	}

