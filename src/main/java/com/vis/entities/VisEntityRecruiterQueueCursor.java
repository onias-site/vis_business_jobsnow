package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Where the queue of the e-mail of resumes stopped: the last recruiter who had their turn, among the ones who did not
 * view a resume in the last 35 days. The next run starts right after this position and, at the end of the queue, goes
 * back to its beginning, so whoever received the e-mail and viewed nothing gives their place to the next one.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityRecruiterQueueCursor.Fields.class)
public class VisEntityRecruiterQueueCursor implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityRecruiterQueueCursor INSTANCE = new VisEntityRecruiterQueueCursor();

	private VisEntityRecruiterQueueCursor() {}

	/** The entity {@code vis_recruiter_queue_cursor}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityRecruiterQueueCursor.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code cursorName} field: the primary key, the queue this position belongs to ({@link CursorNames}). */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = CursorNames.class)
		cursorName,

		/**
		 * The {@code lastPosition} field: required; in the queue {@code resumesToRecruiters}, the digit of the group of the
		 * recruiter (sign-up first, then the others) followed by the SHA-1 of their e-mail; in the queue
		 * {@code legacyViewersOfResumesToRecruiters}, 13 digits that put the most recent legacy view first followed by the
		 * SHA-1. The SHA-1 is the signed hexadecimal of {@code CcpHashDecorator}: up to 40 digits, without the zeros on the
		 * left, and a minus sign when negative, so its length varies (until 2026-11-07 the field demanded exactly 41
		 * characters and refused the position of many recruiters).
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 60)
		lastPosition,
	}

	/** The values of the {@code cursorName} field. */
	public static enum CursorNames implements CcpJsonFieldName{
		/** The queue of the e-mail of resumes to the recruiters. */
		resumesToRecruiters,
		/** The queue of the recruiters who viewed a resume in the legacy JobsNow, in the same e-mail. */
		legacyViewersOfResumesToRecruiters,
	}
}
