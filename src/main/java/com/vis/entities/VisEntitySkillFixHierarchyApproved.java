package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillFixHierarchyApproved.Fields.class)
public class VisEntitySkillFixHierarchyApproved implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyApproved.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email, 

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		explanation,

		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		description,
	
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,

	}
}
