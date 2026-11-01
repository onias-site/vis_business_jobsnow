package com.vis.business.telegram;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * Calculates again, from the index of the positions of Telegram, the {@code ranking} and the {@code childOcurrences} of
 * {@link VisEntitySkill}, the only fields of that entity this process writes. {@code occurrences} are the positions that
 * have the skill. Each skill of a position gives 1 to {@code childOcurrences} of each of its ancestors, once per ancestor
 * even when reached by two paths; a parent with no record gets nothing. The order is that of
 * {@link VisSkillRankingComparator}, from 1 on. Nothing is accumulated between loads: everything comes from the index.
 *
 * <p>It is a batch process run by hand after the load, not an online request.
 *
 * <p>Output: {@code skillsRanked} and {@code skillsWithOccurrences}.
 */
public class VisBusinessRecalculateSkillRanking implements CcpBusiness {

	/** The texts of the process. */
	enum Constants implements CcpJsonFieldName {
		/** How long each page of the reading of the skills stays open. */
		SCROLL_TIME { public String getValue() { return "1m"; } },
	}

	/** How many skills each page of the reading brings, and how many go in each bulk. */
	private static final int PAGE_SIZE = 1_000;

	/** The single instance: the business keeps no state. */
	public static final VisBusinessRecalculateSkillRanking INSTANCE = new VisBusinessRecalculateSkillRanking();

	private VisBusinessRecalculateSkillRanking() {}

	/**
	 * Recalculates the ranking.
	 * @param json not read
	 * @return the input and the counters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Map<String, CcpJsonRepresentation> skillsByName = getSkillsByName();
		List<CcpJsonRepresentation> positions = VisTelegramPositionsIndex.readAll();

		Map<String, Integer> occurrences = new HashMap<>();
		Map<String, Integer> childOcurrences = new HashMap<>();
		Map<String, Set<String>> ancestorsBySkill = new HashMap<>();
		String skillsSeparator = VisEntityTelegramPosition.Constants.SKILLS_SEPARATOR.getValue();
		for (CcpJsonRepresentation position : positions) {
			String joinedSkills = position.getAsString(VisEntityTelegramPosition.Fields.skill);
			String[] skillsOfThePosition = joinedSkills.split(skillsSeparator);
			List<String> skills = Arrays.asList(skillsOfThePosition);
			for (String skill : skills) {
				occurrences.merge(skill, 1, Integer::sum);
				Set<String> ancestors = ancestorsBySkill.computeIfAbsent(skill, x -> getAncestors(x, skillsByName));
				for (String ancestor : ancestors) {
					childOcurrences.merge(ancestor, 1, Integer::sum);
				}
			}
		}

		List<CcpJsonRepresentation> skillsToSort = new ArrayList<>();
		Set<String> skillNames = skillsByName.keySet();
		for (String skillName : skillNames) {
			CcpJsonRepresentation skill = skillsByName.get(skillName);
			Integer occurrencesOfTheSkill = occurrences.getOrDefault(skillName, 0);
			Integer childOcurrencesOfTheSkill = childOcurrences.getOrDefault(skillName, 0);
			CcpJsonRepresentation skillWithOccurrences = skill.put(VisTelegramLoadFields.occurrences, occurrencesOfTheSkill);
			CcpJsonRepresentation skillToSort = skillWithOccurrences.put(VisEntitySkill.Fields.childOcurrences, childOcurrencesOfTheSkill);
			skillsToSort.add(skillToSort);
		}
		skillsToSort.sort(VisSkillRankingComparator.INSTANCE);

		writeRanking(skillsToSort);

		int skillsCount = skillsToSort.size();
		int skillsWithOccurrencesCount = occurrences.keySet().stream().filter(skillNames::contains).toArray().length;
		CcpJsonRepresentation jsonWithSkills = json.put(VisTelegramLoadFields.skillsRanked, skillsCount);
		CcpJsonRepresentation jsonWithOccurrences = jsonWithSkills.put(VisTelegramLoadFields.skillsWithOccurrences, skillsWithOccurrencesCount);
		return jsonWithOccurrences;
	}

	/**
	 * Every skill, with its id.
	 * @return the skills by name, in upper case
	 */
	private static Map<String, CcpJsonRepresentation> getSkillsByName() {
		List<CcpJsonRepresentation> skills = new ArrayList<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		CcpQueryOptions everySkill = CcpQueryOptions.INSTANCE.matchAll();
		everySkill.selectFrom(VisEntitySkill.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, skills::add);

		Map<String, CcpJsonRepresentation> skillsByName = new HashMap<>();
		for (CcpJsonRepresentation skill : skills) {
			String skillName = skill.getAsString(VisEntitySkill.Fields.skill);
			String upperCaseSkillName = skillName.toUpperCase();
			skillsByName.put(upperCaseSkillName, skill);
		}
		return skillsByName;
	}

	/**
	 * The ancestors of a skill that have a record: it walks the whole chain of parents, safe from cycles.
	 * @param skillName the skill
	 * @param skillsByName the skills
	 * @return each ancestor once
	 */
	private static Set<String> getAncestors(String skillName, Map<String, CcpJsonRepresentation> skillsByName) {
		Set<String> ancestors = new LinkedHashSet<>();
		Set<String> visited = new HashSet<>();
		visited.add(skillName);
		Deque<String> pending = new ArrayDeque<>();
		pending.add(skillName);
		while (false == pending.isEmpty()) {
			String current = pending.poll();
			boolean noRecord = false == skillsByName.containsKey(current);

			if(noRecord) {
				continue;
			}

			CcpJsonRepresentation skill = skillsByName.get(current);
			List<String> parents = skill.getAsStringList(VisEntitySkill.Fields.parent);
			for (String parent : parents) {
				// by the exact name: the suffix 123 of some names is part of the name of the record (SECURITY123)
				String parentName = parent.toUpperCase();
				boolean alreadyVisited = false == visited.add(parentName);

				if(alreadyVisited) {
					continue;
				}

				pending.add(parentName);
				boolean parentHasNoRecord = false == skillsByName.containsKey(parentName);

				if(parentHasNoRecord) {
					continue;
				}

				ancestors.add(parentName);
			}
		}
		return ancestors;
	}

	/**
	 * Writes the position of each skill: an update of only the two fields, which keeps the rest of the record.
	 * @param sortedSkills the skills, in the order of the ranking
	 */
	private static void writeRanking(List<CcpJsonRepresentation> sortedSkills) {
		Collection<CcpBulkItem> items = new ArrayList<>();
		int position = 0;
		for (CcpJsonRepresentation skill : sortedSkills) {
			position++;
			String skillId = skill.getAsString(JnJsonCommonsFields.id);
			Integer childOcurrences = skill.getAsIntegerNumber(VisEntitySkill.Fields.childOcurrences);
			CcpJsonRepresentation newValuesWithRanking = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.ranking, position);
			CcpJsonRepresentation newValues = newValuesWithRanking.put(VisEntitySkill.Fields.childOcurrences, childOcurrences);
			CcpBulkItem item = new CcpBulkItem(newValues, CcpBulkEntityOperationType.update, VisEntitySkill.ENTITY, skillId);
			items.add(item);
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDeleteKeysFromCache.INSTANCE);
	}
}
