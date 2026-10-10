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
import com.vis.business.skill.VisSkillSuggestionDecisions;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The last decision of the support bot operator on each suggested skill, whoever suggested it: one record per
 * {@code skill}, with the {@code status} ({@link VisSkillSuggestionDecisions}), the operator's justification
 * ({@code explanation}) and the synonyms of the reviewed suggestion. Written when a suggestion leaves
 * {@link VisEntitySkillPending} for {@link VisEntitySkillApproved} or {@link VisEntitySkillRejected}. A skill already
 * reviewed is not sent to the operator again when another candidate suggests it: that candidate sees this decision
 * instead. Neither the email nor the justification of the candidate who suggested it are kept here, since this record
 * is shown to every candidate. 1-hour cache.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillReviewed.Fields.class)
public class VisEntitySkillReviewed implements CcpEntityConfigurator {

	/** The entity {@code vis_skill_reviewed}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillReviewed.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code skill} field: the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill,

		/** The {@code synonym} field: validated as in {@code VisJsonCommonsFields}, list. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorArray
		synonym,

		/** The {@code status} field: the operator's decision, one of {@link VisSkillSuggestionDecisions}, required. */
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSkillSuggestionDecisions.class)
		@CcpJsonFieldValidatorRequired
		status,

		/** The {@code explanation} field: the support bot operator's justification, validated as in {@code JnJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		explanation,
		;
	}
}
