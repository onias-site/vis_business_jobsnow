package com.vis.business.skill;

import java.util.ArrayList;

import com.vis.business.telegram.VisSkillWordsInMemory;

/**
 * The dictionary of skills read by a process at a given moment: its version ({@code VisEntitySkillDictionary}), the moment
 * it was read and the words of the skills, indexed by their initials. Immutable: a new reading is a new snapshot.
 */
final class VisSkillDictionarySnapshot {

	/** The longest a process keeps a snapshot without reading the skills again (1 hour), even with the same version. */
	static final long MAXIMUM_AGE = 3_600_000L;

	/** The snapshot before the first reading: no version, so the first request reads the skills. */
	static final VisSkillDictionarySnapshot NONE = new VisSkillDictionarySnapshot(-1L, 0L, new VisSkillWordsInMemory(new ArrayList<>()));

	/** The version of the dictionary this snapshot was read from. */
	final long version;

	/** The moment (milliseconds) of the reading. */
	final long loadedAt;

	/** The words of the skills. */
	final VisSkillWordsInMemory words;

	VisSkillDictionarySnapshot(long version, long loadedAt, VisSkillWordsInMemory words) {
		this.version = version;
		this.loadedAt = loadedAt;
		this.words = words;
	}

	/**
	 * Tells whether this snapshot still serves: same version and younger than {@link #MAXIMUM_AGE}.
	 * @param currentVersion the version stored now
	 * @param now the current moment (milliseconds)
	 * @return {@code true} when it serves
	 */
	boolean isUpToDate(long currentVersion, long now) {
		boolean sameVersion = this.version == currentVersion;
		long age = now - this.loadedAt;
		boolean young = age < MAXIMUM_AGE;
		boolean upToDate = sameVersion && young;
		return upToDate;
	}
}
