package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.annotations.JnEntityDisposable;
import com.jn.entities.decorators.builders.JnEntityDisposableBuilder;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Representa o agrupamento paginado das percepções/avaliações registradas sobre um currículo específico,
 * organizado pelo e-mail do candidato.
 * Possui cache de 24 horas e é expurgável anualmente.
 */
@CcpEntityCache(86400)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityDisposableBuilder.class, priority = 1),})
@JnEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.yearly)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityGroupResumesPerceptionsByResume.Fields.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
public class VisEntityGroupResumesPerceptionsByResume implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityGroupResumesPerceptionsByResume.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		email, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		listSize, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		from, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		detail
		;
	}
}
