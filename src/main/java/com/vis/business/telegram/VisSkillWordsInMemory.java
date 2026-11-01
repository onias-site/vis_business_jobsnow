package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
		for (CcpJsonRepresentation item : items) {
			String word = item.getAsString(VisJsonCommonsFields.word);
			String initials = word.substring(0, VisEntityVirtualSkillInitialsGrouper.SHORTEST_WORD);
			indexedItems.computeIfAbsent(initials, x -> new ArrayList<>()).add(item);
		}
		this.itemsByInitials = indexedItems;
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
