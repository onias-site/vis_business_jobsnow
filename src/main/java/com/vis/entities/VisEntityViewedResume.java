package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerLinkedinHash;

/**
 * The resumes viewed by each recruiter: one record per resume ({@code linkedin}) and recruiter ({@code email}). Both
 * keys are kept only as SHA-1 hashes (LGPD): this system avoids writing personal data in a readable form, so the
 * recruiter refers to the resume by an {@code alias} of their own choosing.
 */
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsValidator(VisEntityViewedResume.Fields.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
public class VisEntityViewedResume implements CcpEntityConfigurator {

	/** The entity {@code vis_viewed_resume}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityViewedResume.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code linkedin} field: part of the primary key, the LinkedIn address of the viewed resume; stored as its
		 * SHA-1 hash by {@code VisJsonTransformerLinkedinHash}.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(regexValidation = "^https://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-_%]*[a-zA-Z0-9_%]/?$")
		@CcpEntityFieldTransformer(VisJsonTransformerLinkedinHash.class)
		linkedin,

		/**
		 * The {@code email} field: part of the primary key, the e-mail of the recruiter who viewed the resume, validated
		 * as in {@code JnJsonCommonsFields}; stored as its SHA-1 hash by the {@code email} transformer of
		 * {@code JnJsonTransformersFieldsEntityDefault}.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/**
		 * The {@code alias} field: required, text of 5 to 200 characters, the nickname the recruiter gave to the resume,
		 * since the resume itself is not kept in a readable form.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 5, maxLength = 200)
		alias,
	}
}
