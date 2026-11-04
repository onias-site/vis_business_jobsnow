package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * The version of the dictionary of skills that each process of the API keeps in memory to read the skills of a text
 * ({@code VisSkillDictionary}), with the ids of every {@link VisEntitySkill}. One record, read by id (OQL) on each request:
 * when its {@code version} differs from the one in memory, the process reads the skills again by their ids (MGIE). Every
 * save of a skill writes a new version ({@code VisBusinessUpdateSkillDictionary}). The seed comes with the seed of the
 * skills; in production, {@code documentation/deploy/24-dicionario-de-skills.py} created it. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntitySkillDictionary.Fields.class)
public class VisEntitySkillDictionary implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntitySkillDictionary INSTANCE = new VisEntitySkillDictionary();

	private VisEntitySkillDictionary() {}

	/** The entity {@code vis_skill_dictionary}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillDictionary.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code dictionaryName} field: the primary key, one of {@link DictionaryNames}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = DictionaryNames.class)
		dictionaryName,

		/** The {@code version} field: required, the moment (milliseconds) of the last change of a skill. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNumberUnsigned(minValue = 0)
		version,

		/** The {@code skillIds} field: required, the id of every skill. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 41)
		skillIds,
		;
	}

	/** The values of the {@code dictionaryName} field. */
	public static enum DictionaryNames implements CcpJsonFieldName{
		/** The dictionary of the skills. */
		skills,
	}

	/**
	 * The primary key of the dictionary of skills.
	 * @return the JSON with {@code dictionaryName}
	 */
	public static CcpJsonRepresentation getKeyOfTheSkills() {
		String skills = DictionaryNames.skills.name();
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON.put(Fields.dictionaryName, skills);
		return key;
	}
}
