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
 * {@code parent} array of the stored record. Saving it also moves the skill between the groups of initials of the lookup
 * of the resume screen ({@link VisSkillWordsByInitials}), when a parent with new initials enters or the last parent with
 * some initials leaves: the decorator of {@code @CcpEntityGrouperChild} of {@link VisEntitySkill} does it.
 */
class VisBusinessChangeTheSkillParents implements CcpBusiness {

	/** Applies the approved item to a set of parents. */
	private final Consumer<Set<String>> changeParents;

	/**
	 * Keeps the change to apply.
	 * @param changeParents applies the approved item to a set of parents
	 */
	VisBusinessChangeTheSkillParents(Consumer<Set<String>> changeParents) {
		this.changeParents = changeParents;
	}

	/**
	 * Changes the parents of the stored skill.
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
		return jsonWithTheStoredSkill;
	}
}
