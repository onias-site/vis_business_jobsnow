package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.skill.VisSkillWordsByInitials;
import com.vis.entities.VisEntityVirtualSkillInitialsGrouper;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The lookup of {@link VisSkillWordsByInitials#getItems} kept in memory: for hundreds of thousands of texts, the two reads
 * of that lookup per text would take days. The items are read once and indexed by the initials of their words, so each
 * text gets the same items it would get from the groups. An instance is immutable, so the threads of the load share it.
 */
public class VisSkillWordsInMemory implements Function<Collection<String>, List<CcpJsonRepresentation>> {

	/** The items of the words, by their initials. */
	private final Map<String, List<CcpJsonRepresentation>> itemsByInitials;

	/** The skills that have each skill as a parent, by the parent, in upper case. */
	private final Map<String, Set<String>> childrenBySkill;

	/** The skills of the items, in upper case: the skills the dictionary has as skills of their own, not only as parents. */
	private final Set<String> skills;

	/**
	 * Reads the items of every word of every skill.
	 */
	public VisSkillWordsInMemory() {
		this(VisSkillWordsByInitials.getAllItems());
	}

	/**
	 * Indexes the items.
	 * @param items the items of the words of the skills
	 */
	public VisSkillWordsInMemory(List<CcpJsonRepresentation> items) {
		Map<String, List<CcpJsonRepresentation>> indexedItems = new HashMap<>();
		Map<String, Set<String>> childrenOfEachSkill = new HashMap<>();
		Set<String> skillsOfTheItems = new HashSet<>();
		for (CcpJsonRepresentation item : items) {
			String word = item.getAsString(VisJsonCommonsFields.word);
			String initials = word.substring(0, VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD);
			indexedItems.computeIfAbsent(initials, x -> new ArrayList<>()).add(item);
			String skill = item.getAsString(VisJsonCommonsFields.skill);
			String upperCaseSkill = skill.toUpperCase();
			skillsOfTheItems.add(upperCaseSkill);
			List<String> parents = item.getAsStringList(VisJsonCommonsFields.parent);
			for (String parent : parents) {
				String upperCaseParent = parent.toUpperCase();
				childrenOfEachSkill.computeIfAbsent(upperCaseParent, x -> new LinkedHashSet<>()).add(upperCaseSkill);
			}
		}
		this.itemsByInitials = indexedItems;
		this.childrenBySkill = childrenOfEachSkill;
		this.skills = skillsOfTheItems;
	}

	/**
	 * Whether the dictionary has the skill as a skill of its own (a record of {@code vis_skill}). A name that only appears
	 * as a parent of other items, such as an old name of a renamed skill, is not one: no search finds resumes by it.
	 * @param skill the skill, in any case
	 * @return true when the skill is the skill of some item
	 */
	public boolean hasTheSkill(String skill) {
		String upperCaseSkill = skill.toUpperCase();
		boolean known = this.skills.contains(upperCaseSkill);
		return known;
	}

	/**
	 * The skill and every skill below it in the hierarchy: its children (the skills that have it as a parent), the children
	 * of its children and so on. A resume with any of them has the skill (a "backend" position takes the "php" resumes).
	 * @param skill the skill, in any case
	 * @return the skill and its descendants, in upper case
	 */
	public Set<String> getSkillAndItsDescendants(String skill) {
		String upperCaseSkill = skill.toUpperCase();
		Set<String> found = new LinkedHashSet<>();
		found.add(upperCaseSkill);
		List<String> pending = new ArrayList<>();
		pending.add(upperCaseSkill);
		while (false == pending.isEmpty()) {
			int lastIndex = pending.size() - 1;
			String current = pending.remove(lastIndex);
			Set<String> noChildren = new LinkedHashSet<>();
			Set<String> children = this.childrenBySkill.getOrDefault(current, noChildren);
			for (String child : children) {
				boolean newChild = found.add(child);

				if(newChild) {
					pending.add(child);
				}
			}
		}
		return found;
	}

	/**
	 * The items whose initials are the ones of the given words.
	 * @param words the words of the text, in upper case
	 * @return the items
	 */
	public List<CcpJsonRepresentation> apply(Collection<String> words) {
		Set<String> initialsOfTheWords = new LinkedHashSet<>();
		for (String word : words) {
			int wordLength = word.length();
			boolean wordIsTooShort = wordLength < VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD;

			if(wordIsTooShort) {
				continue;
			}

			String initials = word.substring(0, VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD);
			initialsOfTheWords.add(initials);
		}

		List<CcpJsonRepresentation> items = new ArrayList<>();
		for (String initials : initialsOfTheWords) {
			boolean noItems = false == this.itemsByInitials.containsKey(initials);

			if(noItems) {
				continue;
			}

			List<CcpJsonRepresentation> itemsOfTheInitials = this.itemsByInitials.get(initials);
			items.addAll(itemsOfTheInitials);
		}
		return items;
	}
}
