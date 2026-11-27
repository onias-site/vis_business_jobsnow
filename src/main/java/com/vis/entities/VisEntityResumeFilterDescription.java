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

/**
 * The text of the position that a logged user shared with the link of a search of resumes (since 2026-11-26): in the
 * screen {@code curriculos/buscar}, "Copiar este link para enviar para alguém" with the text of the position filled in
 * (and, when the user is not logged, the confirmation of putting it in the link) saves the text here, under the id of the
 * filters ({@link VisEntityResumeFilter}) and the e-mail of the user, and the link gets {@code &email=<e-mail>}. Opened
 * with both parameters, the screen reads the filters and this text in one union all
 * ({@code VisBusinessGetTheResumeFilter}), fills the text in and shows the skills found in it. The same user sharing the
 * same filters again replaces the text.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityResumeFilterDescription.Fields.class)
public class VisEntityResumeFilterDescription implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityResumeFilterDescription INSTANCE = new VisEntityResumeFilterDescription();

	private VisEntityResumeFilterDescription() {}

	/** The entity {@code vis_resume_filter_description}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeFilterDescription.class).entityInstance;

	/** The fields of the entity, with their validation rules. */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The id of the filters ({@code vis_resume_filter}): the SHA-1 the search answered, up to 40 hexadecimal
		 * characters, with the sign of the hash of the Java when negative ({@code CcpHashDecorator}: 41 characters).
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 41, regexValidation = "^-?[0-9a-fA-F]+$")
		filterId,

		/** The e-mail of the logged user who shared the link, stored as its hash. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The text of the position (or the description of the skills), from 100 to 5,000 characters, as the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 100, maxLength = 5000)
		description,
		;
	}
}
