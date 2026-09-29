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
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.annotations.JnEntityAsyncWriter;
import com.jn.entities.decorators.builders.JnEntityAsyncWriterBuilder;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Records a recruiter's last view of a resume, storing a complete snapshot of the resume and of the
 * position at the moment of the view, plus flags telling whether the resume was negativated and
 * whether the position was inactive. Has asynchronous writing and a 1-hour cache.
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8),})
@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityResumeLastView.Fields.class)
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
