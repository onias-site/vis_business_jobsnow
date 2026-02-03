package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntityTwin(
		twinEntityName = "skill_approved"
		,afterRecordBeenTransportedFromTwinToMainEntity = {},
		 afterRecordBeenTransportedFromMainToTwinEntity = {}
		)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntitySkillPending.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {},//FIXME ENVIO DA SKILL POR TELEGRAM
		afterSaveRecord = {},//FIXME ALTERAR DADOS DA DATA QUANDO PASSAR PRA TWIN
				//FIXME ENVIO DE EMAIL QUANDO REJEITAR SKILL
				//FIXME ENVIO DE EMAIL QUANDO APROVAR SKILL
				//FIXME TRANSFORMAR APPROVED EM SKILL
		flow = {}
)
public class VisEntitySkillPending implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillPending.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill, 

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		parent,
	
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email, 
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,

		;
	}
}
