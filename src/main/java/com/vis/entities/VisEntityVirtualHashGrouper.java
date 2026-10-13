package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonHashCombinationFields;

/**
 * Virtual entity used to calculate a composite hash that represents one search a resume satisfies: a single
 * combination of resume type, region, skill, spoken language and level, seniority, travel, negotiable claim and
 * disability flag. Every field is part of the primary key and holds a single exact value, so the id of each combination
 * works as the hash; the numeric ranges (claims, availability, temporary job time) stay out of it and are compared
 * after the read by id. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityVirtualHashGrouper.Fields.class)
public class VisEntityVirtualHashGrouper{

	/** The entity {@code vis_virtual_hash_grouper}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualHashGrouper.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code travel} field: part of the primary key, validated as in {@code VisJsonHashCombinationFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)
		travel,
		/** The {@code negotiableClaim} field: part of the primary key, validated as in {@code VisJsonHashCombinationFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)
		negotiableClaim,
		/** The {@code pcd} field: part of the primary key, validated as in {@code VisJsonHashCombinationFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)
		pcd,
		/** The {@code ddd} field: part of the primary key, validated as in {@code VisJsonHashCombinationFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)
		ddd,
		/** The {@code skillName} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skillName,
		/** The {@code languageName} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		languageName,
		/** The {@code languageLevel} field: part of the primary key, validated as in {@code VisJsonHashCombinationFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)
		languageLevel,
		/** The {@code resumeType} field: part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,
		/** The {@code seniority} field: part of the primary key, validated as in {@code VisJsonHashCombinationFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonHashCombinationFields.class)
		seniority,
		;
	}
}
