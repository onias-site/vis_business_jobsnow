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

/**
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} of one skill the owners of the resumes do not
 * want their resumes to be found by: its child is a JSON text with how many active resumes exclude the skill now
 * ({@code @CcpEntityGrouperChildCounter} of {@link VisEntityResume} with
 * {@code JnGrouperChildCurrentCounterEntityBeforeWrite}), so the most avoided skills can be known. Its only field is a
 * single skill, one per item of the {@code skill} list of the resume whose category is {@code excludedSkills}. Cached
 * for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualExcludedSkillCounter.Fields.class)
public class VisEntityVirtualExcludedSkillCounter {

	/** The entity {@code vis_virtual_excluded_skill_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualExcludedSkillCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code skill} field: the primary key, one excluded skill (a text, not a list), validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill,
		;
	}
}
