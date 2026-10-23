package com.vis.business.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkill;

/**
 * Action of {@code VisBusinessSkillFixHierarchyItem} for a skill with a {@code VisEntitySkill} record: changes the
 * {@code parent} array of the stored record and the parents of the skill and of each synonym in the lookup of the resume
 * screen ({@link VisSkillWordsGroups}).
 */
class VisBusinessChangeTheSkillAndTheLookup implements CcpBusiness {

	/** The skill of the approved item. */
	private final String skill;

	/** The words of the skill in the lookup; the synonyms of the stored record are added to them. */
	private final List<String> words;

	/** Applies the approved item to a set of parents. */
	private final Consumer<Set<String>> changeParents;

	/**
	 * Keeps the change to apply.
	 * @param skill the skill of the approved item
	 * @param words the words of the skill in the lookup
	 * @param changeParents applies the approved item to a set of parents
	 */
	VisBusinessChangeTheSkillAndTheLookup(String skill, List<String> words, Consumer<Set<String>> changeParents) {
		this.skill = skill;
		this.words = words;
		this.changeParents = changeParents;
	}

	/**
	 * Changes the parents of the stored skill and of its words in the lookup.
	 * @param jsonWithTheStoredSkill the json of the procedure, holding the skill record under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredSkill) {
		CcpJsonRepresentation skillRecord = jsonWithTheStoredSkill.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkill.ENTITY);
		List<String> currentParents = skillRecord.getAsStringList(VisEntitySkill.Fields.parent);
		Set<String> parents = new LinkedHashSet<>(currentParents);

		this.changeParents.accept(parents);

		List<String> updatedParents = new ArrayList<>(parents);
		CcpJsonRepresentation skillRecordWithUpdatedParents = skillRecord.put(VisEntitySkill.Fields.parent, updatedParents);
		VisEntitySkill.ENTITY.save(skillRecordWithUpdatedParents);

		List<String> synonyms = skillRecord.getAsStringList(VisEntitySkill.Fields.synonym);
		this.words.addAll(synonyms);
		VisSkillWordsGroups.changeParents(this.skill, this.words, this.changeParents);
		return jsonWithTheStoredSkill;
	}
}
