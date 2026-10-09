package com.vis.business.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillApproved;

/**
 * Callback of the transfer of a {@code VisEntitySkillPending} suggestion to {@link VisEntitySkillApproved},
 * executed after the transfer has actually happened, that is, when the support bot operator approves the skill.
 * Puts the skill in {@link VisEntitySkill}: a new skill gets the suggested synonyms and the last {@code ranking} (the
 * number of skills plus one, since the ranking goes from the most to the least frequent in the resumes); a skill that
 * is already there (approved meanwhile from another candidate's suggestion) only gets the synonyms it did not have.
 *
 * <p>Also puts the skill and each synonym in {@code VisEntityGroupPositionsBySkills} ({@link VisSkillWordsGroups}), the lookup by the first two
 * letters that the reading of skills from a resume uses: without it the approved skill would never be found in a
 * resume, and a new suggestion of one of its synonyms would not be refused. The cache of each changed group is
 * dropped, since that reading keeps the group in the cache for an hour.
 *
 * <p>Runs in the review of the support bot, never in an online request, so counting the skills is allowed.
 */
public class VisBusinessSkillSuggestionApply implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the approved suggestion
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String skill = json.getAsString(VisEntitySkillApproved.Fields.skill);
		List<String> suggestedSynonyms = json.getAsStringList(VisEntitySkillApproved.Fields.synonym);
		this.saveSkill(skill, suggestedSynonyms);

		VisSkillWordsGroups.addWord(skill, skill);

		for (String synonym : suggestedSynonyms) {
			VisSkillWordsGroups.addWord(skill, synonym);
		}

		return json;
	}

	/**
	 * Creates the skill in {@link VisEntitySkill}, or adds the synonyms it did not have.
	 * @param skill the skill
	 * @param suggestedSynonyms the synonyms suggested with it
	 */
	private void saveSkill(String skill, List<String> suggestedSynonyms) {

		CcpJsonRepresentation skillKey = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.skill, skill);
		boolean skillIsNew = false == VisEntitySkill.ENTITY.exists(skillKey);

		if(skillIsNew) {
			long skillsCount = CcpQueryOptions.INSTANCE
					.matchAll()
					.selectFrom(VisEntitySkill.ENTITY)
					.total();
			long ranking = skillsCount + 1;
			CcpJsonRepresentation newSkillWithSynonyms = skillKey.put(VisEntitySkill.Fields.synonym, suggestedSynonyms);
			CcpJsonRepresentation newSkill = newSkillWithSynonyms.put(VisEntitySkill.Fields.ranking, ranking);
			VisEntitySkill.ENTITY.save(newSkill);
			return;
		}

		CcpJsonRepresentation skillRecord = VisEntitySkill.ENTITY.getOneById(skillKey);
		List<String> currentSynonyms = skillRecord.getAsStringList(VisEntitySkill.Fields.synonym);
		Set<String> synonyms = new LinkedHashSet<>(currentSynonyms);
		synonyms.addAll(suggestedSynonyms);
		List<String> updatedSynonyms = new ArrayList<>(synonyms);
		CcpJsonRepresentation skillRecordWithSynonyms = skillRecord.put(VisEntitySkill.Fields.synonym, updatedSynonyms);
		VisEntitySkill.ENTITY.save(skillRecordWithSynonyms);
	}

}
