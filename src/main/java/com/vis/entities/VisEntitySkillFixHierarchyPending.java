package com.vis.entities;

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
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.JnAsyncWriterEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.templates.email.VisEmailTemplates;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityDataTransfers(
		globalHandlers = {},
		transfers = {
				@CcpEntityDataTransfer(from = mainEntity, to = VisEntitySkillFixHierarchyRejected.class, transferType = transferDataTo, when = after, execute = {VisEmailTemplates.RejectedSkillHierarchy.class}, transferHandlers = {}),
				@CcpEntityDataTransfer(from = mainEntity, to = VisEntitySkillFixHierarchyApproved.class, transferType = transferDataTo, when = after, execute = {VisEmailTemplates.AprovedSkillHierarchy.class}, transferHandlers = {}),
		}
		)

@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillFixHierarchyPending.Fields.class)
public class VisEntitySkillFixHierarchyPending implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyPending.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email, 

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		description,
	
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

	}
}
