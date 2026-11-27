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
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} of one skill (since 2026-11-26): its child is
 * the list of the ids of the resumes that have the skill, directly (written in the resume, category
 * {@code skillsFromText}) or indirectly (an ancestor of a skill written in it, category {@code parent}: a resume with
 * ASPNET is in the groups of ASPNET, DOTNET and BACKEND). A resume is a child of one group per skill, so crossing the
 * groups of several skills (one read by id each) gives the resumes that have every skill of a list. The active resumes
 * are in the main groups, the inactive ones in the twin groups. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualSkillGrouper.Fields.class)
public class VisEntityVirtualSkillGrouper {

	/** The entity {@code vis_virtual_skill_grouper}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualSkillGrouper.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code skill} field: the primary key, one skill (a text, not a list), validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill,
		;
	}
}
