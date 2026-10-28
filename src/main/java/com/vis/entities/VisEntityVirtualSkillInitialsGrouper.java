package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Virtual entity whose id identifies a group of skills of {@code JnEntityGrouper}: the first two letters, in upper case,
 * of a word that names a skill. A {@link VisEntitySkill} is a child of the group of the initials of each of its words (the
 * skill itself, each synonym and each parent), so the reading of skills from a text finds, by id, the skills whose words
 * start like the words of the text. Until 2026-10-27 the entity {@code vis_group_positions_by_skills} kept this lookup.
 * Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualSkillInitialsGrouper.Fields.class)
public class VisEntityVirtualSkillInitialsGrouper {

	/** The entity {@code vis_virtual_skill_initials_grouper}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualSkillInitialsGrouper.class).entityInstance;

	/** The shortest word that has a group: a word needs two letters to have initials. */
	public static final int SHORTEST_WORD = 2;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code firstTwoInitials} field: the primary key, the first two letters of a word in upper case. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(exactLength = 2)
		firstTwoInitials,
		;
	}

	/**
	 * The combination of the group of a word: its first two letters, in upper case.
	 * @param word a word of at least {@link #SHORTEST_WORD} characters
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination(String word) {
		String upperCaseWord = word.toUpperCase();
		String firstTwoInitials = upperCaseWord.substring(0, SHORTEST_WORD);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.firstTwoInitials, firstTwoInitials);
		return combination;
	}

	/**
	 * The parent id of the group of a word, the {@code parentId} of its group in {@code JnEntityGrouper}.
	 * @param word a word of at least {@link #SHORTEST_WORD} characters
	 * @return the parent id
	 */
	public static String getParentId(String word) {
		CcpJsonRepresentation combination = getCombination(word);
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}
}
