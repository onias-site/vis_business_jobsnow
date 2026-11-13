package com.vis.entities;

import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.business.bots.receive.messages.VisUserReceiveMessagesBotConfiguration;

/**
 * The wrong login tokens typed in the command {@code /receiveMessages} of the user bot (since 2026-11-12), one record
 * per chat ({@code chatId}): {@code attempts} counts them, 1 at the first one. A chat that reaches the limit of
 * attempts ({@code JnSystemProperties.maxAttempts()}, 3) can no longer use the command; a right token deletes the record.
 * <p>
 * Seeds the command {@code /receiveMessages} ({@link VisUserReceiveMessagesBotConfiguration}).
 */
@CcpEntityCache(3600)
@CcpEntityFieldsValidator(VisEntityLoginTokenAttempts.Fields.class)
public class VisEntityLoginTokenAttempts implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityLoginTokenAttempts INSTANCE = new VisEntityLoginTokenAttempts();

	private VisEntityLoginTokenAttempts() {}

	/** The entity {@code vis_login_token_attempts}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityLoginTokenAttempts.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName {
		/** The {@code chatId} field: the primary key, the chat of the user bot, validated as in {@code JnJsonInstantMessengerFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonInstantMessengerFields.class)
		chatId,
		/** The {@code attempts} field: required, the wrong tokens typed so far (1 at the first one), validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		attempts,
		;
	}

	/**
	 * The command {@code /receiveMessages} of the user bot.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		List<CcpBulkItem> botSeeds = VisUserReceiveMessagesBotConfiguration.getSeeds();
		return botSeeds;
	}
}
