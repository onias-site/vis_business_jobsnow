package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnGrouperChildFieldEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildFieldEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.transformers.VisPositionInterestHourCombinations;
import com.vis.json.transformers.VisPositionInterestedResumesCombinations;

/**
 * The interest of a candidate in a position (since 2026-11-24): in the screen "Meus dados → Minhas vagas" the candidate
 * logged in confirms that they want to send their resume to a position open to the candidates ({@code byTheCandidates}),
 * and the resume goes to the recruiter in the next full hour ({@code sendingHour}, {@code yyyyMMddHH}), with the resumes
 * of the other candidates who did the same before that hour, in the e-mail of the lists of resumes of the positions. One
 * record per candidate and position: a second confirmation is refused. The save keeps, in {@code jn_grouper}, the id of
 * the resume in the group of the position and hour ({@link VisEntityVirtualPositionInterestedResumes}) and the id of the
 * position in the group of the hour ({@link VisEntityVirtualPositionInterestHour}), both read by id by the process of
 * every full hour. Written asynchronously (AE).
 */
@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityPositionInterest.Fields.class)
// the resumes interested in the position, in the group of the position and hour of the sending
@CcpEntityGrouperChild(value = {VisEntityVirtualPositionInterestedResumes.class}, beforeDecoratorClass = JnGrouperChildFieldEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildFieldEntityAfterWrite.class, childField = "resumeId")
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualPositionInterestedResumes.class, value = VisPositionInterestedResumesCombinations.class)
// the positions with interested resumes in the hour of the sending, in the group of the hour
@CcpEntityGrouperChild(value = {VisEntityVirtualPositionInterestHour.class}, beforeDecoratorClass = JnGrouperChildFieldEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildFieldEntityAfterWrite.class, childField = "position")
@CcpEntityGrouperParentCombinations(parent = VisEntityVirtualPositionInterestHour.class, value = VisPositionInterestHourCombinations.class)
public class VisEntityPositionInterest implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityPositionInterest INSTANCE = new VisEntityPositionInterest();

	private VisEntityPositionInterest() {}

	/** The entity {@code vis_position_interest}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPositionInterest.class).entityInstance;

	/** The fields of the entity, with their validation rules. */
	public static enum Fields implements CcpJsonFieldName{
		/** The id of the position ({@code vis_position}). */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		position,

		/** The e-mail of the candidate, stored as its SHA-1 hash, as {@code vis_resume} keeps it. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The id of the resume of the candidate ({@code vis_resume}), the child of the group of the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		resumeId,

		/** The full hour when the resume goes to the recruiter, {@code yyyyMMddHH}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10)
		sendingHour,
		;
	}
}
