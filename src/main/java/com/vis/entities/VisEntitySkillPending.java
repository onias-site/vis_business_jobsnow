package com.vis.entities;

import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDecoratorOperationType.save;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDecoratorTransferType.transferDataTo;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityOperationStepType.after;
import static com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityType.mainEntity;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDataTransfer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDataTransfers;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityOperation;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityOperations;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.JnAsyncWriterEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.resume.skills.VisBusinessApprovingSkill;
import com.vis.business.templates.email.VisEmailTemplates;
import com.vis.business.templates.notify.support.VisTemplatesToNotifySupport;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityOperations(
		operations = {
				@CcpEntityOperation(when = after, operation = save, into = mainEntity,  execute = {VisTemplatesToNotifySupport.NewSkill.class, VisEmailTemplates.PedingSkillHierarchy.class}, operationHandlers = {}),
		},
		globalHandlers = {}
		)

@CcpEntityDataTransfers(
		globalHandlers = {},
		transfers = {
				@CcpEntityDataTransfer(from = mainEntity, to = VisEntitySkillRejected.class, transferType = transferDataTo, when = after, execute = {VisEmailTemplates.RejectedSkill.class}, transferHandlers = {}),
				@CcpEntityDataTransfer(from = mainEntity, to = VisEntitySkill.class, transferType = transferDataTo, when = after, execute = {VisBusinessApprovingSkill.class, VisEmailTemplates.AprovedSkill.class}, transferHandlers = {}),
		}
		)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillPending.Fields.class)
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
