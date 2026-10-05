package com.vis.entities;

import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenTransferOperationType.afterTransferDataFromMainEntitySendAnEmailMessageAndIfFailsThrowAnError;
import static com.jn.entities.decorators.enums.JnEntitySendMessageToUserWhenWriteOperationType.afterInsertFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError;

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
 * Represents newly suggested skills awaiting approval. On save, it notifies support via
 * VisTemplatesToNotifySupport.NewSkill and sends a pending-status message via VisMessages.PendingSkillHierarchy.
 * The entity supports transferring the data to VisEntitySkillRejected or to VisEntitySkill according to
 * the decision. Has asynchronous writing and a 1-hour cache.
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
			operationType = afterInsertFromMainEntitySendAnEmailMessageAndInstantMessageAndIfFailsThrowAnError,
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
				targetEntity = VisEntitySkill.class
			),
		}
		)

@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillPending.Fields.class)
public class VisEntitySkillPending implements CcpEntityConfigurator {

	/** The entity {@code vis_skill_pending}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillPending.class).entityInstance;
	
	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code date} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		
		/** The {@code email} field: validated as in {@code JnJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email, 

		/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, list. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		parent,
	
		/** The {@code ranking} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ranking,
		
		/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, part of the primary key. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill, 

		/** The {@code synonym} field: validated as in {@code VisJsonCommonsFields}, list. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,
		
		/** The {@code timestamp} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

		;
	}
}
