package com.vis.entities;

import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterSaveFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError;

import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenTransferOperationType.afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
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
import com.vis.messages.VisMessages;
import com.vis.messages.VisMessages.VisNotifyUserAboutAprovedSkillHierarchy;
import com.vis.messages.VisMessages.VisNotifyUserAboutRejectedSkillHierarchy;

/**
 * Representa solicitações pendentes de correção de hierarquia de skill aguardando análise. Ao salvar um
 * registro, dispara transferência de dados para VisEntitySkillFixHierarchyRejected ou
 * VisEntitySkillFixHierarchyApproved conforme a decisão, enviando mensagens de notificação correspondentes.
 * Possui escrita assíncrona e cache de 1 hora.
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
			messageTemplate =  VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.class
			),
})
@JnEntitySendMessageToUserWhenTransfer(
		{
			@JnEntitySendMessageToUserWhenTransferOperation
			(
				operationType = afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError,
				messageTemplate = VisNotifyUserAboutRejectedSkillHierarchy.class,
				targetEntity = VisEntitySkillFixHierarchyRejected.class
			),
			@JnEntitySendMessageToUserWhenTransferOperation
			(
				operationType = afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError,
				messageTemplate = VisEntitySkillFixHierarchyApproved.class,
				targetEntity = VisNotifyUserAboutAprovedSkillHierarchy.class
			),
		}
		)

@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
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
