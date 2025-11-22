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

//FIXME CHAMAR LOGIN
//FIXME BUG DE VOLTAR PRA ABA ANTERIOR
//FIXME DESABILITAR ABAS
//FIXME SOMENTE HOMEOFFICE
//FIXME TODAS AS REGIOES METROPOLITANAS
//FIXME MULTI CHECKBOX DA REGIAO METROPOLITANA
//FIXME CHIPS DE EMPRESAS INDESEJADAS
//FIXME VALIDAÇOES DAS ABAS  
//FIXME PCD
//FIXME DISPONIBILITY
//FIXME CORRIGIR PRETENSOES
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
			CcpJsonRepresentation result = VisEntityResume.ENTITY.save(sessionValues);

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
	enum Save{
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		companiesNotAllowed,//ENCONTRADO
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		clt,//ENCONTRADO
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		btc,//ENCONTRADO
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		ddd,//ENCONTRADO
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		desiredJob,//ENCONTRADO
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		disabilities,//ENCONTRADO
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility,
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,//ENCONTRADO
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		experience,//ENCONTRADO
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		lastJob,//ENCONTRADO
		@CcpJsonCopyFieldValidationsFrom(VisEntityResume.Fields.class)
		pj,//ENCONTRADO
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		fileName, //ENCONTRADO
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		observations, //ENCONTRADO
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisBusinessSaveResumeInBucket.JsonFieldNames.class)
		resumeBase64 //ENCONTRADO
	
	}

