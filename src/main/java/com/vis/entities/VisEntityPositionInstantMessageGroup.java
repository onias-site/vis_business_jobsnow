package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.defaultvalues.annotations.CcpJsonFieldDefaultValue;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.business.position.VisBusinessPutPositionInstantMessageGroupDefaultValues;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The instant message groups (since 2026-11-23) where a position of a seniority, an area ({@code resumeType}) and a ddd
 * is published: one record per combination, holding the chats of the groups and the bot that writes to them. Each group
 * of positions covers one area, so its chat is in the records of every seniority and every ddd of that area.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code vis_position_instant_message_group}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityPositionInstantMessageGroup.Fields.class)
// the default value of botType (user) goes into the record before it is saved
@CcpBeforeSaveFromMainEntity(execute = {VisBusinessPutPositionInstantMessageGroupDefaultValues.class}, operationHandlers = {})
public class VisEntityPositionInstantMessageGroup implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityPositionInstantMessageGroup INSTANCE = new VisEntityPositionInstantMessageGroup();

	private VisEntityPositionInstantMessageGroup() {}

	/** The entity {@code vis_position_instant_message_group}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPositionInstantMessageGroup.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code seniority} field: part of the primary key, one of {@code VisSeniorityType}, as in the positions. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityType.class)
		seniority,

		/** The {@code resumeType} field: part of the primary key, the area, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,

		/** The {@code ddd} field: part of the primary key, one ddd, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/** The {@code chatId} field: required, list of at least one chat of group, each validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		chatId,

		/** The {@code botType} field: required, the bot that writes to the groups, one of {@code JnBotType}, {@code user} by default. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = JnMessageType.JnBotType.class)
		@CcpJsonFieldDefaultValue(defaultStrings = "user")
		botType,
	}
}
