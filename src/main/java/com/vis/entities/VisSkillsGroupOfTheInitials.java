package com.vis.entities;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Function of {@link VisEntityGroupPositionsBySkills}: builds the record of the group of the skills whose words start with
 * the given first two letters.
 */
class VisSkillsGroupOfTheInitials implements Function<String, CcpJsonRepresentation> {

	/** The skills grouped by the first two letters of their words. */
	private final CcpJsonRepresentation groupedSkills;

	/**
	 * Keeps where the groups come from.
	 * @param groupedSkills the skills grouped by the first two letters of their words
	 */
	VisSkillsGroupOfTheInitials(CcpJsonRepresentation groupedSkills) {
		this.groupedSkills = groupedSkills;
	}

	/**
	 * Builds the record of one group.
	 * @param initials the first two letters
	 * @return the record of the group
	 */
	public CcpJsonRepresentation apply(String initials) {
		CcpFieldName initialsKey = new CcpFieldName(initials);
		List<CcpJsonRepresentation> skill = this.groupedSkills.getAsJsonList(initialsKey);
		CcpJsonRepresentation jsonWithSkills = CcpOtherConstants.EMPTY_JSON
				.put(VisJsonCommonsFields.skill, skill);
		CcpJsonRepresentation json = jsonWithSkills
				.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, initials)
				;
		return json;
	}
}
