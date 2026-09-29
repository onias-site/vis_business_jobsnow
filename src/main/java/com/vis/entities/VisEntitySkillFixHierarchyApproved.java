package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionablePurgeBuilder;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Records the approved skill hierarchy fix requests, storing the requester's e-mail, the implicit
 * knowledge ({@code parent}), the skill, the fix type ({@code add} or {@code remove}), the approval
 * explanation and the fix description. Versionable, with a 1-hour cache.
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityVersionablePurgeBuilder.class, priority = 5),})
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillFixHierarchyApproved.Fields.class)
public class VisEntitySkillFixHierarchyApproved implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyApproved.class).entityInstance;

	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		explanation,

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		description,

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		parent,

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldValidatorRequired
		skill,

		@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldValidatorRequired
		type,

	}
}
