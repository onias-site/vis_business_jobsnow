package com.vis.business.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntityVirtualSkillInitialsGrouper;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisSkillInitialsCombinations;

/**
 * Reads the lookup by the first two letters of a word that the reading of skills from a text uses: the groups of
 * {@link VisEntityVirtualSkillInitialsGrouper} in {@code JnEntityGrouper}, whose children are the ids of the
 * {@link VisEntitySkill} records that have a word (the skill itself, a synonym or a parent) with those initials. Each
 * word found becomes an item that tells which {@code skill} the {@code word} means, with the {@code parent} list and the
 * {@code synonym} list of the skill. The groups are written by the decorator of {@code @CcpEntityGrouperChild} of
 * {@link VisEntitySkill} on every save of a skill, so the approvals of the support bot reach the lookup by themselves.
 * Until 2026-10-27 the entity {@code vis_group_positions_by_skills} kept the items, written by {@code VisSkillWordsGroups}.
 */
public final class VisSkillWordsByInitials {

	/** Utility class; not instantiable. */
	private VisSkillWordsByInitials() {}

	/**
	 * The items of every word, of the skills in the groups of the initials of the given words, whose initials are among
	 * them: two reads by id, the groups and then the skills. A parent that is no skill of its own is an item of itself,
	 * with no parents nor synonyms.
	 * @param words the words whose initials are looked up (a word shorter than two characters has none)
	 * @return the items: {@code skill}, {@code word} (upper case), {@code parent} and {@code synonym}
	 */
	public static List<CcpJsonRepresentation> getItems(Collection<String> words) {
		Set<String> parentIds = new LinkedHashSet<>();
		for (String word : words) {
			int wordLength = word.length();
			boolean wordIsTooShort = wordLength < VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD;

			if(wordIsTooShort) {
				continue;
			}

			String parentId = VisEntityVirtualSkillInitialsGrouper.getParentId(word);
			parentIds.add(parentId);
		}

		List<CcpJsonRepresentation> skills = getSkillsOfTheGroups(parentIds);

		Set<String> skillNames = new LinkedHashSet<>();
		for (CcpJsonRepresentation skill : skills) {
			String skillName = skill.getAsString(VisEntitySkill.Fields.skill);
			String upperCaseSkillName = skillName.toUpperCase();
			skillNames.add(upperCaseSkillName);
		}

		List<CcpJsonRepresentation> items = new ArrayList<>();
		Map<String, CcpJsonRepresentation> itemsOfParentsWithoutSkill = new LinkedHashMap<>();
		for (CcpJsonRepresentation skill : skills) {
			String skillName = skill.getAsString(VisEntitySkill.Fields.skill);
			List<String> parents = skill.getAsStringList(VisEntitySkill.Fields.parent);
			List<String> synonyms = skill.getAsStringList(VisEntitySkill.Fields.synonym);

			List<String> namesOfTheSkill = new ArrayList<>();
			namesOfTheSkill.add(skillName);
			namesOfTheSkill.addAll(synonyms);
			for (String name : namesOfTheSkill) {
				boolean outOfTheGroups = isOutOfTheGroups(name, parentIds);

				if(outOfTheGroups) {
					continue;
				}

				CcpJsonRepresentation item = getItem(skillName, name, parents, synonyms);
				items.add(item);
			}

			for (String parent : parents) {
				String upperCaseParent = parent.toUpperCase();
				boolean parentIsASkill = skillNames.contains(upperCaseParent);

				if(parentIsASkill) {
					continue;
				}

				boolean outOfTheGroups = isOutOfTheGroups(parent, parentIds);

				if(outOfTheGroups) {
					continue;
				}

				List<String> noWords = new ArrayList<>();
				CcpJsonRepresentation item = getItem(parent, parent, noWords, noWords);
				itemsOfParentsWithoutSkill.put(upperCaseParent, item);
			}
		}

		Collection<CcpJsonRepresentation> parentItems = itemsOfParentsWithoutSkill.values();
		items.addAll(parentItems);
		return items;
	}

	/**
	 * Tells whether a word is a known skill: the skill itself, a synonym or a parent of a skill in the group of its first
	 * two letters (upper case), read by id through the common read of the groups.
	 * @param word the word
	 * @return 0 when the word is known, 1 when there is no group for its initials (a word shorter than two characters has
	 * none), 2 when the group exists but does not have it
	 */
	public static int getWordStatus(String word) {
		int wordLength = word.length();
		boolean wordIsTooShort = wordLength < VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD;

		if(wordIsTooShort) {
			return 1;
		}

		String parentId = VisEntityVirtualSkillInitialsGrouper.getParentId(word);
		String parentName = getParentName();
		String childName = getChildName();
		List<String> skillIds = JnGroupers.getChildren(false, parentName, childName, parentId);
		boolean groupNotFound = skillIds.isEmpty();

		if(groupNotFound) {
			return 1;
		}

		List<CcpJsonRepresentation> skills = getSkills(skillIds);
		String upperCaseWord = word.toUpperCase();
		for (CcpJsonRepresentation skill : skills) {
			List<String> wordsOfTheSkill = VisSkillInitialsCombinations.getWords(skill);
			for (String wordOfTheSkill : wordsOfTheSkill) {
				String upperCaseWordOfTheSkill = wordOfTheSkill.toUpperCase();
				boolean sameWord = upperCaseWordOfTheSkill.equals(upperCaseWord);

				if(sameWord) {
					return 0;
				}
			}
		}

		return 2;
	}

	/**
	 * The skills of the groups, read by id: the groups in one round trip, then the skills in another.
	 * @param parentIds the parent ids of the groups
	 * @return the skills, each one once
	 */
	private static List<CcpJsonRepresentation> getSkillsOfTheGroups(Set<String> parentIds) {
		String parentName = getParentName();
		String childName = getChildName();
		Map<String, List<String>> skillIdsByParentId = JnGroupers.getChildrenOfGroups(parentName, childName, parentIds);
		Set<String> skillIds = new LinkedHashSet<>();
		Collection<List<String>> skillIdsOfEachGroup = skillIdsByParentId.values();
		for (List<String> skillIdsOfTheGroup : skillIdsOfEachGroup) {
			skillIds.addAll(skillIdsOfTheGroup);
		}
		List<CcpJsonRepresentation> skills = getSkills(skillIds);
		return skills;
	}

	/**
	 * The skills of the given ids, read in one round trip.
	 * @param skillIds the ids of the skills
	 * @return the skills found
	 */
	private static List<CcpJsonRepresentation> getSkills(Collection<String> skillIds) {
		CcpEntityMetaData skillDetails = VisEntitySkill.ENTITY.getEntityMetaData();
		CcpJsonRepresentation skillsById = skillDetails.getMultipleByRecordIds(skillIds);
		List<CcpJsonRepresentation> skills = new ArrayList<>();
		Set<String> foundIds = skillsById.fieldSet();
		for (String skillId : foundIds) {
			CcpFieldName skillIdFieldName = new CcpFieldName(skillId);
			CcpJsonRepresentation skill = skillsById.getInnerJson(skillIdFieldName);
			skills.add(skill);
		}
		return skills;
	}

	/**
	 * Tells whether the initials of a word are out of the groups read (or the word has none).
	 * @param word the word
	 * @param parentIds the parent ids of the groups read
	 * @return {@code true} when the word belongs to none of them
	 */
	private static boolean isOutOfTheGroups(String word, Set<String> parentIds) {
		int wordLength = word.length();
		boolean wordIsTooShort = wordLength < VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD;

		if(wordIsTooShort) {
			return true;
		}

		String parentId = VisEntityVirtualSkillInitialsGrouper.getParentId(word);
		boolean inTheGroups = parentIds.contains(parentId);
		return false == inTheGroups;
	}

	/**
	 * An item of the lookup.
	 * @param skill the skill the word means
	 * @param word the word
	 * @param parents the parents of the skill
	 * @param synonyms the synonyms of the skill
	 * @return the item
	 */
	private static CcpJsonRepresentation getItem(String skill, String word, List<String> parents, List<String> synonyms) {
		String upperCaseWord = word.toUpperCase();
		CcpJsonRepresentation itemWithSkill = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.skill, skill);
		CcpJsonRepresentation itemWithWord = itemWithSkill.put(VisJsonCommonsFields.word, upperCaseWord);
		CcpJsonRepresentation itemWithParents = itemWithWord.put(VisJsonCommonsFields.parent, parents);
		CcpJsonRepresentation item = itemWithParents.put(VisJsonCommonsFields.synonym, synonyms);
		return item;
	}

	/**
	 * The kind of the groups of the lookup ({@code parentName} of {@code JnEntityGrouper}).
	 * @return the name of {@link VisEntityVirtualSkillInitialsGrouper}
	 */
	private static String getParentName() {
		CcpEntityMetaData groupDetails = VisEntityVirtualSkillInitialsGrouper.ENTITY.getEntityMetaData();
		return groupDetails.entityName;
	}

	/**
	 * The entity of the children of the groups of the lookup ({@code childName} of {@code JnEntityGrouper}).
	 * @return the name of {@link VisEntitySkill}
	 */
	private static String getChildName() {
		CcpEntityMetaData skillDetails = VisEntitySkill.ENTITY.getEntityMetaData();
		return skillDetails.entityName;
	}
}
