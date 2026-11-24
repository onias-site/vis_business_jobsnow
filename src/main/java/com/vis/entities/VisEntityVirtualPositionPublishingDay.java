package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * The virtual entity that gives the id of the groups of the positions to publish in the groups of instant messages on each
 * day (since 2026-11-24): one group of {@code jn_grouper} per day ({@code yyyyMMdd}), whose children are the positions with
 * {@code mustSendInGroups} still valid on that day, each one with the bot that publishes it. The process of 10:00 reads
 * the group of the day by id. Not a table of its own.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualPositionPublishingDay.Fields.class)
public class VisEntityVirtualPositionPublishingDay {

	/** The entity, used only to calculate the id of the groups and to name them. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualPositionPublishingDay.class).entityInstance;

	/** The fields of the entity. */
	public static enum Fields implements CcpJsonFieldName{
		/** The day of the publishing, {@code yyyyMMdd}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 8, maxLength = 8)
		day,
		;
	}
}