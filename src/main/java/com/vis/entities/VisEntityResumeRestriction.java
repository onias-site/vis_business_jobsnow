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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * The resumes that must be corrected by their owners (since 2026-11-12), one record per owner ({@code email}, kept as
 * its SHA-1 like the resume, so the id of the record is the id of the resume): {@code message} tells what is wrong. While
 * the record exists the resume goes to no recruiter and its link opens nothing (404), and the form of the owner shows the
 * messages in red at the top. Any update of the resume, active or inactive, deletes the record: the save only passes with
 * every field valid. The first records came from the domains of {@code notAllowedCompany} saved before that field was
 * validated (script 65 of the deploy).
 * <p>
 * Configuration:
 * <ul>
 * <li>index {@code vis_resume_restriction}</li>
 * <li>records cached for 3600 seconds</li>
 * </ul>
 */
@CcpEntityCache(3600)
@CcpEntityFieldsValidator(VisEntityResumeRestriction.Fields.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
public class VisEntityResumeRestriction implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityResumeRestriction INSTANCE = new VisEntityResumeRestriction();

	private VisEntityResumeRestriction() {}

	/** The entity {@code vis_resume_restriction}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeRestriction.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName {
		/** The {@code email} field: the primary key, the e-mail of the owner of the resume, kept as its SHA-1. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,
		/** The {@code message} field: required, at least one text of 10 to 2000 characters, what the owner must correct. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 2000)
		message,
		;
	}
}
