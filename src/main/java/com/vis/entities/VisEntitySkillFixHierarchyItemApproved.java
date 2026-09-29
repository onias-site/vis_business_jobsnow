package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Represents each approved skill of a skill hierarchy fix request, one record per skill, with exactly the
 * fields of {@link VisEntitySkillFixHierarchyItemPending}, from which the records are transferred.
 * 1-hour cache.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillFixHierarchyItemApproved.Fields.class)
public class VisEntitySkillFixHierarchyItemApproved implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyItemApproved.class).entityInstance;

	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		parent,

		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill,

		@CcpJsonFieldTypeString(allowedValuesEnum = VisSkillFixHierarchyTypes.class)
		@CcpEntityFieldPrimaryKey
		type,

	}
}
