package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * The instant message groups where the positions are published (since 2026-11-23), one record per chat: the name and the
 * link the recruiter sees in the position form. Which group gets which position is in
 * {@code vis_position_instant_message_group}, which holds only the chats.
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code vis_instant_message_group}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityInstantMessageGroup.Fields.class)
public class VisEntityInstantMessageGroup implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityInstantMessageGroup INSTANCE = new VisEntityInstantMessageGroup();

	private VisEntityInstantMessageGroup() {}

	/** The entity {@code vis_instant_message_group}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityInstantMessageGroup.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code chatId} field: the primary key, the chat of the group, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		chatId,

		/** The {@code title} field: required, the name of the group. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 255)
		title,

		/** The {@code link} field: required, the public or invite link of the group ({@code https://t.me/...}). */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(regexValidation = "https://t\\.me/\\S+", minLength = 14, maxLength = 200)
		link,
	}
}
