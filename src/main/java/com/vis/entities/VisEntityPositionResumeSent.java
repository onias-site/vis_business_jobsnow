package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * The resumes already sent to a position that receives them instantly (since 2026-11-24): the e-mail "Cv recebido para a
 * vaga" goes once per pair position and resume, and the next saves of the same resume (each edition) find the pair here
 * and send nothing.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityPositionResumeSent.Fields.class)
public class VisEntityPositionResumeSent implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityPositionResumeSent INSTANCE = new VisEntityPositionResumeSent();

	private VisEntityPositionResumeSent() {}

	/** The entity {@code vis_position_resume_sent}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPositionResumeSent.class).entityInstance;

	/** The fields of the entity, with their validation rules. */
	public static enum Fields implements CcpJsonFieldName{
		/** The id of the position (of {@code vis_position}). */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		position,

		/** The resume: the e-mail of its owner, hashed as {@code vis_resume} keeps it. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		resume,
		;
	}
}
