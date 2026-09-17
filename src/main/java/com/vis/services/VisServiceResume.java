package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.entities.VisEntityResume;


/**
 * Serviço de acesso a dados de currículos. Expõe as operações de CRUD sobre a entidade VisEntityResume.
 */
public enum VisServiceResume implements JnService {
	ChangeStatus{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityResume.ENTITY.delete(json);

			return  json;
		}
	},
	Delete{
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			VisEntityResume.ENTITY.delete(sessionValues);

			return sessionValues;
		}
	},
	GetData{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation changeStatus = VisEntityResume.ENTITY.getOneByIdAnyWhere(json);
			
			return changeStatus;
		}
	},
	Save{
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			
			VisEntityResume.ENTITY.save(sessionValues);

			return sessionValues;
		}
		
		public Class<?> getJsonValidationClass() {
			return VisEntityResume.Fields.class;
		}
	}, 
	;
}
