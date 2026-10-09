package com.vis.business.skill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.vis.entities.VisEntityGroupPositionsBySkills;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Writes in {@link VisEntityGroupPositionsBySkills}, the lookup by the first two letters of a word that the reading of
 * skills from a resume uses: each item tells which {@code skill} a {@code word} (the skill itself or a synonym) means
 * and the skill's {@code parent} list, which becomes the implicit knowledge shown in the resume screen. The reviews
 * of the support bot change {@code VisEntitySkill}, and without writing here too the resume screen would never show
 * what was approved. The cache of every changed group is dropped, since that reading keeps a group in the cache for
 * an hour.
 */
public final class VisSkillWordsGroups {

	/** Utility class; not instantiable. */
	private VisSkillWordsGroups() {}

	/**
	 * Adds the word, pointing to its skill, to the group of its first two letters, unless the group already has it.
	 * @param skill the skill the word means
	 * @param word the skill itself or one of its synonyms
	 */
	public static void addWord(String skill, String word) {

		String upperCaseWord = word.toUpperCase();
		CcpJsonRepresentation groupKey = getGroupKey(upperCaseWord);
		List<CcpJsonRepresentation> items = getItems(groupKey);
		Stream<CcpJsonRepresentation> itemsStream = items.stream();
		boolean wordAlreadyInTheGroup = itemsStream.anyMatch(item -> upperCaseWord.equals(item.getAsString(VisJsonCommonsFields.word)));

		if(wordAlreadyInTheGroup) {
			return;
		}

		CcpJsonRepresentation itemWithSkill = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.skill, skill);
		CcpJsonRepresentation item = itemWithSkill.put(VisJsonCommonsFields.word, upperCaseWord);
		List<CcpJsonRepresentation> updatedItems = new ArrayList<>(items);
		updatedItems.add(item);
		saveItems(groupKey, updatedItems);
	}

	/**
	 * Changes the {@code parent} list of every item of the skill in the groups of the given words.
	 * @param skill the skill whose items change
	 * @param words the skill itself and its synonyms, the words whose groups may have items of the skill
	 * @param changeParents what to do with the parents of each item (add or remove one)
	 */
	public static void changeParents(String skill, List<String> words, Consumer<Set<String>> changeParents) {

		Set<String> groupsInitials = new LinkedHashSet<>();

		for (String word : words) {
			String upperCaseWord = word.toUpperCase();
			String firstTwoInitials = upperCaseWord.substring(0, 2);
			groupsInitials.add(firstTwoInitials);
		}

		for (String firstTwoInitials : groupsInitials) {
			CcpJsonRepresentation groupKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, firstTwoInitials);
			List<CcpJsonRepresentation> items = getItems(groupKey);
			List<CcpJsonRepresentation> updatedItems = new ArrayList<>();
			boolean groupChanged = false;

			for (CcpJsonRepresentation item : items) {
				String itemSkill = item.getAsString(VisJsonCommonsFields.skill);
				boolean otherSkill = false == skill.equals(itemSkill);

				if(otherSkill) {
					updatedItems.add(item);
					continue;
				}

				List<String> currentParents = item.getAsStringList(VisJsonCommonsFields.parent);
				Set<String> parents = new LinkedHashSet<>(currentParents);
				changeParents.accept(parents);
				List<String> updatedParents = new ArrayList<>(parents);
				CcpJsonRepresentation updatedItem = item.put(VisJsonCommonsFields.parent, updatedParents);
				updatedItems.add(updatedItem);
				groupChanged = true;
			}

			if(groupChanged) {
				saveItems(groupKey, updatedItems);
			}
		}
	}

	/**
	 * The key of the group of the word.
	 * @param upperCaseWord the word, in upper case
	 * @return the key
	 */
	private static CcpJsonRepresentation getGroupKey(String upperCaseWord) {
		String firstTwoInitials = upperCaseWord.substring(0, 2);
		CcpJsonRepresentation groupKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityGroupPositionsBySkills.Fields.firstTwoInitials, firstTwoInitials);
		return groupKey;
	}

	/**
	 * The items of the group, read by id (an empty list when the group does not exist).
	 * @param groupKey the key of the group
	 * @return the items
	 */
	private static List<CcpJsonRepresentation> getItems(CcpJsonRepresentation groupKey) {
		CcpEntityMetaData entityMetaData = VisEntityGroupPositionsBySkills.ENTITY.getEntityMetaData();
		CcpJsonRepresentation group = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(groupKey, notFound -> CcpOtherConstants.EMPTY_JSON);
		List<CcpJsonRepresentation> items = group.getAsJsonList(VisEntityGroupPositionsBySkills.Fields.skill);
		return items;
	}

	/**
	 * Saves the group with the items and drops its cache.
	 * @param groupKey the key of the group
	 * @param items the items
	 */
	private static void saveItems(CcpJsonRepresentation groupKey, List<CcpJsonRepresentation> items) {
		CcpJsonRepresentation updatedGroup = groupKey.put(VisEntityGroupPositionsBySkills.Fields.skill, items);
		VisEntityGroupPositionsBySkills.ENTITY.save(updatedGroup);

		String groupId = VisEntityGroupPositionsBySkills.ENTITY.calculateId(groupKey);
		CcpCacheDecorator groupCache = new CcpCacheDecorator(groupId);
		groupCache.delete();
	}
}
