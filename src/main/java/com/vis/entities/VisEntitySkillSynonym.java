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
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The synonyms given in the skill suggestions, each one pointing to the skill suggested with it: when a candidate
 * suggests PICK BY LIGHT with the synonyms PTL and PUT TO LIGHT, this entity gets {@code {skill: PTL, synonym: PICK BY
 * LIGHT}} and {@code {skill: PUT TO LIGHT, synonym: PICK BY LIGHT}}, written in a single bulk when the suggestion is
 * saved. A suggestion without synonyms writes nothing here.
 * <p>
 * The key is the synonym ({@code skill}), so that the skill typed by the candidate is found by id (no query in an
 * online request): typing PTL, the candidate gets the suggestion of PICK BY LIGHT they already made, instead of
 * suggesting the same skill again through one of its synonyms. 1-hour cache.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntitySkillSynonym.Fields.class)
public class VisEntitySkillSynonym implements CcpEntityConfigurator {

	/** The entity {@code vis_skill_synonym}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillSynonym.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code skill} field: the synonym given in the suggestion, the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		skill,

		/** The {@code synonym} field: the skill suggested with this synonym, validated as in {@code VisJsonCommonsFields}, required. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		synonym,
		;
	}
}
