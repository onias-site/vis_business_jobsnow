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
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;

/**
 * Virtual entity (no index of its own) of a skill in a region and seniority: an item of the accordion of the screen
 * Estatísticas › Skills. Its id names the group of {@code jn_grouper} whose children are the months of the skill
 * ({@link VisEntityStatisSkillLineChart}), read by id (OQL) to draw the line chart. It keeps no value of its own: the
 * total of the skill in a month is in its child.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityStatisSkill.Fields.class)
public class VisEntityStatisSkill {

	/** The entity {@code vis_statis_skill}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityStatisSkill.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code ddd} field: part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		ddd,

		/** The {@code seniority} field: part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		seniority,

		/** The {@code skill} field: part of the primary key, a single skill. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)
		skill,
		;
	}
}
