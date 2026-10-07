package com.vis.business.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * <p>A skill that has no {@link VisEntitySkill} record is skipped, because the record cannot be created without
 * its {@code ranking}.
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

		CcpJsonRepresentation skillKey = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.skill, skill);
		boolean skillDoesNotExist = false == VisEntitySkill.ENTITY.exists(skillKey);

		if(skillDoesNotExist) {
			return json;
		}

		CcpJsonRepresentation skillRecord = VisEntitySkill.ENTITY.getOneById(skillKey);
		List<String> currentParents = skillRecord.getAsStringList(VisEntitySkill.Fields.parent);
		Set<String> parents = new LinkedHashSet<>(currentParents);

		type.execute(parents, parent);

		List<String> updatedParents = new ArrayList<>(parents);
		CcpJsonRepresentation skillRecordWithUpdatedParents = skillRecord.put(VisEntitySkill.Fields.parent, updatedParents);
		VisEntitySkill.ENTITY.save(skillRecordWithUpdatedParents);

		return json;
	}

}
