package com.vis.entities;

import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenTransferOperationType.afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError;
import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterSaveFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.annotations.JnEntityAsyncWriter;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenTransfer;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenTransferOperation;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenWrite;
import com.jn.entities.decorators.annotations.JnEntitySendMessageToUserWhenWriteOperation;
import com.jn.entities.decorators.builders.JnEntityAsyncWriterBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserAfterTransferBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserAfterWriteBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserBeforeTransferBuilder;
import com.jn.entities.decorators.builders.JnEntitySendMessageToUserBeforeWriteBuilder;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.messages.VisMessages;
import com.vis.messages.VisMessages.VisNotifyUserAboutAprovedSkill;
import com.vis.messages.VisMessages.VisNotifyUserAboutRejectedSkill;

/**
 * Representa novas skills sugeridas aguardando aprovação. Ao salvar, notifica o suporte via
 * VisTemplatesToNotifySupport.NewSkill e envia mensagem de pendência via VisMessages.PendingSkillHierarchy.
 * A entidade suporta transferência de dados para VisEntitySkillRejected ou para VisEntitySkill conforme
 * a decisão. Possui escrita assíncrona e cache de 1 hora.
 */
@CcpEntityCache(3600) 

@CcpEntityCustomDecorators(value = {
		@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserBeforeWriteBuilder.class, priority = 7)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserBeforeTransferBuilder.class, priority = 7)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserAfterWriteBuilder.class, priority = 5)
		,@CcpEntityCustomDecorator(value = JnEntitySendMessageToUserAfterTransferBuilder.class, priority = 5)
})

@JnEntitySendMessageToUserWhenWrite({
	@JnEntitySendMessageToUserWhenWriteOperation(
			operationType = afterSaveFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError,
			messageTemplate =  VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class
			),
})
@JnEntitySendMessageToUserWhenTransfer(
		{
			@JnEntitySendMessageToUserWhenTransferOperation
			(
				operationType = afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError,
				messageTemplate = VisNotifyUserAboutRejectedSkill.class,
				targetEntity = VisEntitySkillRejected.class
			),
			@JnEntitySendMessageToUserWhenTransferOperation
			(
				operationType = afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError,
				messageTemplate = VisNotifyUserAboutAprovedSkill.class,
				targetEntity = VisNotifyUserAboutAprovedSkill.class
			),
		}
		)

@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
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
