package com.vis.business.skill;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.business.telegram.VisSkillWordsInMemory;
import com.vis.entities.VisEntitySkillDictionary;

/**
 * The dictionary of skills kept in the memory of the process, used by the reading of skills from a text. Each request
 * reads the version of the dictionary by id ({@link VisEntitySkillDictionary}, cached); when it differs from the version
 * in memory, or the copy in memory is older than 1 hour, the process reads every skill again by its id (MGIE) and swaps
 * the copy at once. The requests that arrive during a reading wait for it, and only one of them reads.
 */
public final class VisSkillDictionary {

	private static final AtomicReference<VisSkillDictionarySnapshot> CURRENT = new AtomicReference<>(VisSkillDictionarySnapshot.NONE);

	private VisSkillDictionary() {}

	/**
	 * The words of the skills, read again first when the version changed or the copy is too old.
	 * @return the words of the skills
	 */
	public static VisSkillWordsInMemory getWords() {
		CcpJsonRepresentation key = VisEntitySkillDictionary.getKeyOfTheSkills();
		CcpJsonRepresentation dictionary = VisEntitySkillDictionary.ENTITY.getOneById(key);
		Long version = dictionary.getAsLongNumber(VisEntitySkillDictionary.Fields.version);
		CcpTimeDecorator time = new CcpTimeDecorator();
		Long now = time.getContent();
		VisSkillDictionarySnapshot current = CURRENT.get();
		boolean upToDate = current.isUpToDate(version, now);

		if(upToDate) {
			return current.words;
		}

		VisSkillWordsInMemory words = read(dictionary, version, now);
		return words;
	}

	private static synchronized VisSkillWordsInMemory read(CcpJsonRepresentation dictionary, long version, long now) {
		VisSkillDictionarySnapshot current = CURRENT.get();
		boolean readMeanwhile = current.isUpToDate(version, now);

		if(readMeanwhile) {
			return current.words;
		}

		List<String> skillIds = dictionary.getAsStringList(VisEntitySkillDictionary.Fields.skillIds);
		List<CcpJsonRepresentation> items = VisSkillWordsByInitials.getItemsOfTheSkillIds(skillIds);
		VisSkillWordsInMemory words = new VisSkillWordsInMemory(items);
		VisSkillDictionarySnapshot snapshot = new VisSkillDictionarySnapshot(version, now, words);
		CURRENT.set(snapshot);
		return words;
	}
}
