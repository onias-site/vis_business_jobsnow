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
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} of the views of the resumes of one region
 * (ddd): every view of a recruiter ({@link VisEntityViewedResume}) whose resume listed the ddd when it was viewed is a
 * child of its group, so a view of a resume with several ddds is a child of several groups. The child is a JSON text with
 * the same part of the resume kept by {@link VisEntityVirtualDddGrouper} (as it was at the view) and the id of the view.
 * A parent of its own, so these groups have a {@code parentName} other than the groups of the resumes. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualViewedResumeDddGrouper.Fields.class)
public class VisEntityVirtualViewedResumeDddGrouper {

	/** The entity {@code vis_virtual_viewed_resume_ddd_grouper}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualViewedResumeDddGrouper.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code ddd} field: the primary key, one region, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,
		;
	}
}
