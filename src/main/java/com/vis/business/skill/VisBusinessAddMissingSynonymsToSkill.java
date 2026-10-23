package com.vis.business.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkill;

/**
 * Action of {@code VisBusinessSkillSuggestionApply} for a skill already in {@link VisEntitySkill}: adds to the stored
 * record the suggested synonyms it did not have.
 */
class VisBusinessAddMissingSynonymsToSkill implements CcpBusiness {

	/** The synonyms suggested with the skill. */
	private final List<String> suggestedSynonyms;

	/**
	 * Keeps the suggested synonyms.
	 * @param suggestedSynonyms the synonyms suggested with the skill
	 */
	VisBusinessAddMissingSynonymsToSkill(List<String> suggestedSynonyms) {
		this.suggestedSynonyms = suggestedSynonyms;
	}

	/**
	 * Adds the missing synonyms.
	 * @param jsonWithTheStoredSkill the json of the procedure, holding the skill record under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonWithTheStoredSkill) {
		CcpJsonRepresentation skillRecord = jsonWithTheStoredSkill.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntitySkill.ENTITY);
		List<String> currentSynonyms = skillRecord.getAsStringList(VisEntitySkill.Fields.synonym);
		Set<String> synonyms = new LinkedHashSet<>(currentSynonyms);
		synonyms.addAll(this.suggestedSynonyms);
		List<String> updatedSynonyms = new ArrayList<>(synonyms);
		CcpJsonRepresentation skillRecordWithSynonyms = skillRecord.put(VisEntitySkill.Fields.synonym, updatedSynonyms);
		VisEntitySkill.ENTITY.save(skillRecordWithSynonyms);
		return jsonWithTheStoredSkill;
	}
}
