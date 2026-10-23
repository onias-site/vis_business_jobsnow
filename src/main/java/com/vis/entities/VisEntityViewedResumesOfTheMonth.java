package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Virtual entity (no index of its own) of the resumes a recruiter viewed in a month: its id, calculated from the e-mail
 * of the recruiter (kept only as its SHA-1, LGPD) in the current month ({@code @CcpEntityDisposable} monthly: the id
 * changes at the turn of the month), names the group of {@code jn_grouper} that holds the views of that month. The
 * {@code viewedResume} list is the {@code child} of that group, filled by {@code @CcpEntityGrouperChild} of
 * {@code vis_viewed_resume}: one JSON text per resume ({@code linkedin}, {@code alias} and {@code date} of the last
 * view), read by id (OQL) by {@code VisBusinessViewedResumesOfTheMonth}, which calculates the id of a past month through
 * {@code disposablePeriod}.
 */
@CcpEntityCache(3600)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.monthly)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityViewedResumesOfTheMonth.Fields.class)
public class VisEntityViewedResumesOfTheMonth {

	/** The entity {@code vis_viewed_resumes_of_the_month}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityViewedResumesOfTheMonth.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code email} field: the primary key, the e-mail of the recruiter at the moment of the views, validated as in
		 * {@code JnJsonCommonsFields}; taken as its SHA-1 hash by the {@code email} transformer of
		 * {@code JnJsonTransformersFieldsEntityDefault}.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/**
		 * The {@code viewedResume} field: required, list of the resumes viewed in the month, each one a JSON text with the
		 * {@code linkedin}, the {@code alias} and the {@code date} ({@code dd/MM/yyyy HH:mm:ss}) of its last view.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray
		viewedResume,
	}
}
