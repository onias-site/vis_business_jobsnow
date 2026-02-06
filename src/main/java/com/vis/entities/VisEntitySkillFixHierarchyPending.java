package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntityDataTransfer;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntityDataTransferRule;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.business.templates.email.VisEmailTemplates;
import com.vis.business.templates.notify.support.VisTemplatesToNotifySupport;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityDataTransfer(rules = {
		@CcpEntityDataTransferRule(whenTransferingToEntity = VisEntitySkillFixHierarchyRejected.class, thenExecuteTheFollowingFlow = {VisEmailTemplates.RejectedSkillHierarchy.class}),
		@CcpEntityDataTransferRule(whenTransferingToEntity = VisEntitySkillFixHierarchyApproved.class, thenExecuteTheFollowingFlow = {VisEmailTemplates.AprovedSkillHierarchy.class})
} )
@CcpEntitySpecifications(
		afterSaveRecord = {},
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntitySkillFixHierarchyPending.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {VisEmailTemplates.PedingSkillHierarchy.class, VisTemplatesToNotifySupport.NewSkillHierarchy.class},
		flow = {}
)
public class VisEntitySkillFixHierarchyPending implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyPending.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email, 

		@CcpJsonFieldTypeString(minLength = 10, maxLength = 500)
		@CcpJsonFieldValidatorRequired
		description,
	
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

	}
}
