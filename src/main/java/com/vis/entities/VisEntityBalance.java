
package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntityDecorators;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.decorators.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityDecorators(JnEntityVersionable.class)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntityBalance.Fields.class,
		cacheableEntity = true, 
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
public class VisEntityBalance implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityBalance.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		email, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned
		balance,
		;
		
	}
}
