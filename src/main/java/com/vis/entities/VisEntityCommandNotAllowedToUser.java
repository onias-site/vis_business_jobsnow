package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.json.fields.validation.VisUserRequestCommands;

/**
 * Users that the support bot operator chose to ignore for a command, because they only play with the requests
 * (absurd requests, swear words): one record per user ({@code email}, kept as hash) and command
 * ({@code commandName}), with the request that led to the decision ({@code description}). The requests of a
 * user found here for a command no longer reach the operator. Queried on every request of the user, hence
 * the 1-hour cache.
 * Has the twin entity vis_command_reallowed_to_user, only for control and tracking: deleting a record (the
 * support bot {@code allowCommandToUser} command) moves it there, so what was undone stays recorded.
 */
@CcpEntityTwin(
		twinEntityName = "vis_command_reallowed_to_user",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityCommandNotAllowedToUser.Fields.class)
public class VisEntityCommandNotAllowedToUser implements CcpEntityConfigurator {

	/** The entity {@code vis_command_not_allowed_to_user}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityCommandNotAllowedToUser.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The {@code commandName} field: part of the primary key, text. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = VisUserRequestCommands.class)
		commandName,

		/** The {@code description} field: nested JSON, required. */
		@CcpJsonFieldTypeNestedJson(allowsEmptyJson = false)
		@CcpJsonFieldValidatorRequired
		description,

	}
}
