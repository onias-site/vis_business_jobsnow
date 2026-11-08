package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisResumeDailyActions;

/**
 * The candidate ({@code email}, kept as hash) ran, in the last 24 hours, the action {@code action}
 * ({@link VisResumeDailyActions}) over the saved resume: one record per candidate and action, written when the action
 * runs and valid for one day from then. While it lasts, the same action is refused with {@code dailyLimitReached} (429)
 * by {@code VisServiceResume}. Each of these actions emails the candidate, so without this limit the screen of the
 * resume could drain the email quota and load the backend. Inserting a resume has no limit.
 * <p>
 * The block lasts 24 hours from the action, not until midnight: after the turn of the day the document of the period no
 * longer matches, but its copy in {@code jn_disposable_record} expires one day after it was written, and
 * {@code JnDisposableEntity.exists} still accepts it until then.
 * <p>
 * Cached: deleting the record straight from the database does not free the day while the cache holds it; delete it
 * through the entity or restart the APIs.
 */
@CcpEntityCache(3600)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityResumeDailyAction.Fields.class)
public class VisEntityResumeDailyAction implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityResumeDailyAction INSTANCE = new VisEntityResumeDailyAction();

	private VisEntityResumeDailyAction() {}

	/** The entity {@code vis_resume_daily_action}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeDailyAction.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code email} field: part of the primary key, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,
		/** The {@code action} field: part of the primary key, the action over the resume ({@link VisResumeDailyActions}). */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = VisResumeDailyActions.class)
		action,
		;
	}

	/**
	 * The key of the action of the candidate, to be checked (by id) before the action and saved after it.
	 * @param email the e-mail of the candidate, as typed
	 * @param action the action over the resume
	 * @return the key
	 */
	public static CcpJsonRepresentation getKey(String email, VisResumeDailyActions action) {
		CcpJsonRepresentation keyWithEmail = CcpOtherConstants.EMPTY_JSON.put(Fields.email, email);
		CcpJsonRepresentation key = keyWithEmail.put(Fields.action, action.name());
		return key;
	}
}
