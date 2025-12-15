package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.global.annotations.CcpJsonCopyGlobalValidationsFrom;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.resume.VisBusinessSaveResumeInBucket;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.utils.VisUtils;
//FIXME CHIPS DE EMPRESAS INDESEJADAS
//FIXME PCD
//FIXME DISPONIBILITY
//FIXME CORRIGIR PRETENSOES
//FIXME VALIDAÇOES DAS ABAS  
//FIXME BUG DE VOLTAR PRA ABA ANTERIOR
//FIXME DESABILITAR ABAS
//FIXME CHAMAR LOGIN
//FIXME TRAZER DADOS
//FIXME DOWNLOAD DE CURRICULO
//FIXME DESABILITAR CURRICULO
//FIXME EXCLUIR CURRICULO

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
	GetFile{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resume = VisUtils.getResumeFromBucket(json);

			return resume;
		}
	},
	Save{
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			
			CcpJsonRepresentation duplicateValueFromField = sessionValues.duplicateValueFromField(com.vis.services.Save.email, com.vis.services.Save.originalEmail);
			
			CcpJsonRepresentation result = VisEntityResume.ENTITY.save(duplicateValueFromField);

			return result;
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
	@CcpJsonCopyGlobalValidationsFrom(VisEntityResume.Fields.class)
	enum Save implements CcpJsonFieldName{
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		companiesNotAllowed,
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		clt,
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		btc,
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		ddd,
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		desiredJob,
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		disabilities,
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility,
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		experience,
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		lastJob,
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		pj,
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		fileName, 
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		observations, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		resumeBase64,
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		originalEmail
	
	}

