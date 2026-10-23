package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Virtual entity (no index of its own) of the views of a resume in a month (the screen "Quem me viu" of the candidate):
 * its id, calculated from the e-mail of the owner of the viewed resume (kept only as its SHA-1, LGPD) and the month of
 * the views ({@code monthYear}), names the group of {@code jn_grouper} that counts them. The child of that group is a
 * single JSON text, filled by {@code @CcpEntityGrouperChildCounter} of {@code vis_viewed_resume}: one field per domain of the
 * recruiters who viewed the resume in the month, with how many of them did it (each recruiter counted once per month),
 * and {@code childCount} is the number of domains. Read by id (OQL) by {@code VisBusinessResumeViewersOfTheMonth}.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityResumeViewsOfTheMonth.Fields.class)
public class VisEntityResumeViewsOfTheMonth {

	/** The entity {@code vis_resume_views_of_the_month}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeViewsOfTheMonth.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code email} field: part of the primary key, the e-mail of the owner of the viewed resume. The views give it
		 * already as its SHA-1 (the e-mails of a LinkedIn address are kept only so, in {@code vis_resume_linkedin}), which
		 * the {@code email} transformer of {@code JnJsonTransformersFieldsEntityDefault} keeps as is; a read by the
		 * readable e-mail of the candidate gets the same hash from it.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 128)
		email,

		/** The {@code monthYear} field: part of the primary key, the month of the views, as {@code yyyyMM}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(regexValidation = "^[0-9]{4}(0[1-9]|1[0-2])$")
		monthYear,
	}
}
