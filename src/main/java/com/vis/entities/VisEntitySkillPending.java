package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntityDataTransferRule;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntityDataTransfer;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.business.resume.skills.VisBusinessApprovingSkill;
import com.vis.business.templates.email.VisEmailTemplates;
import com.vis.business.templates.notify.support.VisTemplatesToNotifySupport;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityDataTransfer(rules = {
		@CcpEntityDataTransferRule(whenTransferingDataToEntity = VisEntitySkillRejected.class, thenExecuteTheFollowingFlow = {VisEmailTemplates.RejectedSkill.class}),
		@CcpEntityDataTransferRule(whenTransferingDataToEntity = VisEntitySkill.class, thenExecuteTheFollowingFlow = {VisBusinessApprovingSkill.class, VisEmailTemplates.AprovedSkill.class})
} )
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntitySkillPending.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {VisTemplatesToNotifySupport.NewSkill.class, VisEmailTemplates.PedingSkillHierarchy.class},
		afterSaveRecord = {},
		flow = {}
)
public class VisEntitySkillPending implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillPending.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email, 

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		parent,
	
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking,
		
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill, 

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

		;
	}
}
