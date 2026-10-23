package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.vis.entities.VisEntitySkill;

/**
 * Action of {@code VisBusinessSkillSuggestionApply} for a skill not yet in {@link VisEntitySkill}: creates it with the
 * suggested synonyms and the last {@code ranking} (the number of skills plus one).
 */
class VisBusinessCreateApprovedSkill implements CcpBusiness {

	/** The key of the skill. */
	private final CcpJsonRepresentation skillKey;

	/** The synonyms suggested with the skill. */
	private final List<String> suggestedSynonyms;

	/**
	 * Keeps the skill to create.
	 * @param skillKey the key of the skill
	 * @param suggestedSynonyms the synonyms suggested with the skill
	 */
	VisBusinessCreateApprovedSkill(CcpJsonRepresentation skillKey, List<String> suggestedSynonyms) {
		this.skillKey = skillKey;
		this.suggestedSynonyms = suggestedSynonyms;
	}

	/**
	 * Creates the skill.
	 * @param skillNotFound the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation skillNotFound) {
		long skillsCount = CcpQueryOptions.INSTANCE
				.matchAll()
				.selectFrom(VisEntitySkill.ENTITY)
				.total();
		long ranking = skillsCount + 1;
		CcpJsonRepresentation newSkillWithSynonyms = this.skillKey.put(VisEntitySkill.Fields.synonym, this.suggestedSynonyms);
		CcpJsonRepresentation newSkill = newSkillWithSynonyms.put(VisEntitySkill.Fields.ranking, ranking);
		VisEntitySkill.ENTITY.save(newSkill);
		return skillNotFound;
	}
}
