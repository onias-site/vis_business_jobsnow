package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.vis.business.skill.VisSkillDictionary;
import com.vis.business.telegram.VisSkillWordsInMemory;
import com.vis.entities.VisEntityJobPosting;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.services.VisServiceSkills;

/**
 * First business before the save of {@code vis_job_posting} (since 2026-11-26): the text of the job posting goes to the
 * same search of skills of {@code skills/fromText}, and the skills it finds, each main one and its parents, each once,
 * go on to the crossing of the groups of the resumes ({@link VisBusinessFindTheMostCoveredSkills}). Only the skills the
 * dictionary has as skills of their own go on (since 2026-11-27): a name that survives only as a parent of old items and
 * in the resumes, such as {@code INTEGRATION} after its rename to {@code INTEGRATION123} in {@code vis_skill}, is in the
 * groups of the resumes but not in the hierarchy of the search, which finds no resume by it and so none at all.
 */
public class VisBusinessFindTheSkillsOfTheJobPosting implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessFindTheSkillsOfTheJobPosting INSTANCE = new VisBusinessFindTheSkillsOfTheJobPosting();

	private VisBusinessFindTheSkillsOfTheJobPosting() {}

	/** The fields this business adds to the record, read by the next ones and not saved. */
	public static enum Fields implements CcpJsonFieldName {
		/** The skills of the text: the main ones and their parents, in the order they were found. */
		requestedSkills,
	}

	/**
	 * Finds the skills of the text.
	 * @param json the record, with {@code description}
	 * @return the record with {@code requestedSkills}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String description = json.getAsString(VisEntityJobPosting.Fields.description);
		CcpJsonRepresentation searchInput = CcpOtherConstants.EMPTY_JSON.put(CcpJsonCommonsFields.text, description);
		CcpJsonRepresentation skillsOfTheText = VisServiceSkills.GetSkillsFromText.execute(searchInput);
		List<CcpJsonRepresentation> foundSkills = skillsOfTheText.getAsJsonList(VisJsonCommonsFields.skill);
		Set<String> requestedSkills = new LinkedHashSet<>();
		for (CcpJsonRepresentation foundSkill : foundSkills) {
			String skillName = foundSkill.getAsString(VisJsonCommonsFields.skill);
			requestedSkills.add(skillName);
			List<String> parents = foundSkill.getAsStringList(VisJsonCommonsFields.parent);
			requestedSkills.addAll(parents);
		}
		VisSkillWordsInMemory words = VisSkillDictionary.getWords();
		List<String> requestedSkillList = new ArrayList<>();
		for (String requestedSkill : requestedSkills) {
			boolean unknown = false == words.hasTheSkill(requestedSkill);

			if(unknown) {
				continue;
			}

			requestedSkillList.add(requestedSkill);
		}
		CcpJsonRepresentation jsonWithTheSkills = json.put(Fields.requestedSkills, requestedSkillList);
		return jsonWithTheSkills;
	}
}
