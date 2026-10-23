package com.vis.business.skill;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Action of {@code VisBusinessSkillFixHierarchyItem} for a skill with no {@code VisEntitySkill} record: only its words
 * in the lookup of the resume screen ({@link VisSkillWordsGroups}) have their parents changed.
 */
class VisBusinessChangeOnlyTheSkillLookup implements CcpBusiness {

	/** The skill of the approved item. */
	private final String skill;

	/** The words of the skill in the lookup. */
	private final List<String> words;

	/** Applies the approved item to a set of parents. */
	private final Consumer<Set<String>> changeParents;

	/**
	 * Keeps the change to apply.
	 * @param skill the skill of the approved item
	 * @param words the words of the skill in the lookup
	 * @param changeParents applies the approved item to a set of parents
	 */
	VisBusinessChangeOnlyTheSkillLookup(String skill, List<String> words, Consumer<Set<String>> changeParents) {
		this.skill = skill;
		this.words = words;
		this.changeParents = changeParents;
	}

	/**
	 * Changes the parents of the words of the skill in the lookup.
	 * @param skillNotFound the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation skillNotFound) {
		VisSkillWordsGroups.changeParents(this.skill, this.words, this.changeParents);
		return skillNotFound;
	}
}
