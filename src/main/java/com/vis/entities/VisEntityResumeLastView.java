package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnAsyncWriterEntity;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntityResumeLastView.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
public class VisEntityResumeLastView implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeLastView.class).entityInstance;

	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 35, maxLength = 50)
		recruiter, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		email, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		@CcpJsonFieldTypeBoolean
		negativatedResume,
		@CcpJsonFieldTypeBoolean
		inactivePosition,
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisEntityResume.Fields.class)
		resume, 
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisEntityPosition.Fields.class)
		position
		;
	}
}
