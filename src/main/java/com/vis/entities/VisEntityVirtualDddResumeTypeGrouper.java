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
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} of one region (ddd) and one area of the resumes
 * (resumeType), since 2026-11-22: every resume that lists the ddd and is of the area is a child of its group, so a resume
 * with several ddds is a child of several groups, all of its own area. The child is a JSON text with part of the resume
 * ({@code childFields} of the {@code @CcpEntityGrouperChild} of {@link VisEntityResume}, the companies the candidate
 * forbids included) and its id. The active resumes are in the main groups, the inactive ones in the twin groups. The
 * position form counts in these groups, by a Painless script, the resumes that satisfy its filters
 * ({@code VisBusinessResumesOfTheFilters}). Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualDddResumeTypeGrouper.Fields.class)
public class VisEntityVirtualDddResumeTypeGrouper {

	/** The entity {@code vis_virtual_ddd_resume_type_grouper}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualDddResumeTypeGrouper.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code ddd} field: part of the primary key, one region, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/** The {@code resumeType} field: part of the primary key, the area of the resumes, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,
		;
	}
}
