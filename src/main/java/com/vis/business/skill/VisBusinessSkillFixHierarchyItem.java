package com.vis.business.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Callback of the transfer of a {@code VisEntitySkillFixHierarchyItemPending} record to
 * {@code VisEntitySkillFixHierarchyItemApproved}, executed after the transfer has actually happened, that is,
 * when the support bot operator approves the item. Applies the approved item to {@link VisEntitySkill}: in the
 * record of its {@code skill}, adds its {@code parent} to the {@code parent} array (association, {@code add}) or
 * removes it from there (dissociation, {@code remove}).
 *
 * <p>Applies the same change to the items of the skill in the lookup the resume screen reads
 * ({@link VisSkillWordsGroups}), in the groups of the skill and of each synonym: up to 2026-10-08 only
 * {@link VisEntitySkill} changed, and the implicit knowledge of the resume screen never showed an approved fix.
 *
 * <p>A skill that has no {@link VisEntitySkill} record is not created there, because the record cannot be created
 * without its {@code ranking}; its own word in the lookup still changes.
 */
public class VisBusinessSkillFixHierarchyItem implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String skill = json.getAsString(VisEntitySkillFixHierarchyItemApproved.Fields.skill);
		String parent = json.getAsString(VisEntitySkillFixHierarchyItemApproved.Fields.parent);
		VisSkillFixHierarchyTypes type = json.getAsEnum(VisEntitySkillFixHierarchyItemApproved.Fields.type, VisSkillFixHierarchyTypes.class);
		Consumer<Set<String>> changeParents = parents -> type.execute(parents, parent);

		List<String> words = new ArrayList<>();
		words.add(skill);

		CcpJsonRepresentation skillKey = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.skill, skill);
		boolean skillDoesNotExist = false == VisEntitySkill.ENTITY.exists(skillKey);

		if(skillDoesNotExist) {
			VisSkillWordsGroups.changeParents(skill, words, changeParents);
			return json;
		}

		CcpJsonRepresentation skillRecord = VisEntitySkill.ENTITY.getOneById(skillKey);
		List<String> currentParents = skillRecord.getAsStringList(VisEntitySkill.Fields.parent);
		Set<String> parents = new LinkedHashSet<>(currentParents);

		changeParents.accept(parents);

		List<String> updatedParents = new ArrayList<>(parents);
		CcpJsonRepresentation skillRecordWithUpdatedParents = skillRecord.put(VisEntitySkill.Fields.parent, updatedParents);
		VisEntitySkill.ENTITY.save(skillRecordWithUpdatedParents);

		List<String> synonyms = skillRecord.getAsStringList(VisEntitySkill.Fields.synonym);
		words.addAll(synonyms);
		VisSkillWordsGroups.changeParents(skill, words, changeParents);

		return json;
	}

}
