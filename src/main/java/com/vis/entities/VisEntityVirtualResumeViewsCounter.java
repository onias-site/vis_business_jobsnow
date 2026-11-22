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
 * Virtual entity (no index of its own) whose id identifies the group of {@code jn_grouper} that counts how many views a
 * resume ever had: its child is a single JSON text, {@code {"views": n}}, filled by {@code @CcpEntityGrouperChildCounter}
 * of {@code vis_viewed_resume} with {@code JnGrouperChildCounterOnBothSidesEntityBeforeWrite} (a history: each recruiter
 * counts once per resume, the later views of the same recruiter, the deactivation and the delete of the view, and a view
 * brought back from the twin do not change it). Its only field is the e-mail of the owner of the resume (kept only as its
 * SHA-1, LGPD), the primary key of {@code vis_resume}, so the id of the parent is the id of the resume. Read by id (OQL).
 * The delete everywhere of the resume deletes the group ({@code VisBusinessDeleteTheViewersOfTheDeletedResume}); its
 * deactivation keeps it. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualResumeViewsCounter.Fields.class)
public class VisEntityVirtualResumeViewsCounter {

	/** The entity {@code vis_virtual_resume_views_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualResumeViewsCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code email} field: the primary key, the e-mail of the owner of the viewed resume. The views give it already
		 * as its SHA-1 (the e-mails of a LinkedIn address are kept only so, in {@code vis_resume_linkedin}), which the
		 * {@code email} transformer of {@code JnJsonTransformersFieldsEntityDefault} keeps as is; a read by the readable
		 * e-mail of the candidate gets the same hash from it.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 128)
		email,
		;
	}
}
