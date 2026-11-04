package com.vis.business.skill;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillDictionary;

/**
 * Runs after each save of a {@link VisEntitySkill} ({@code @CcpAfterSaveFromMainEntity}): writes a new version of
 * {@link VisEntitySkillDictionary}, with the id of the saved skill among its ids, so every process of the API reads the
 * skills again on its next reading of a text. It then removes the record from the cache, so the local processes, which
 * keep a cache each, also see the new version (in production the cache is shared and already has it).
 */
public class VisBusinessUpdateSkillDictionary implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessUpdateSkillDictionary INSTANCE = new VisBusinessUpdateSkillDictionary();

	private VisBusinessUpdateSkillDictionary() {}

	/**
	 * Writes the new version.
	 * @param json the saved skill
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String skillId = VisEntitySkill.ENTITY.calculateId(json);
		CcpJsonRepresentation key = VisEntitySkillDictionary.getKeyOfTheSkills();
		CcpJsonRepresentation dictionary = VisEntitySkillDictionary.ENTITY.getOneById(key);
		List<String> storedSkillIds = dictionary.getAsStringList(VisEntitySkillDictionary.Fields.skillIds);
		List<String> skillIds = new ArrayList<>(storedSkillIds);
		boolean newSkill = false == skillIds.contains(skillId);

		if(newSkill) {
			skillIds.add(skillId);
		}

		CcpTimeDecorator now = new CcpTimeDecorator();
		Long version = now.getContent();
		CcpJsonRepresentation dictionaryWithSkillIds = key.put(VisEntitySkillDictionary.Fields.skillIds, skillIds);
		CcpJsonRepresentation newDictionary = dictionaryWithSkillIds.put(VisEntitySkillDictionary.Fields.version, version);
		VisEntitySkillDictionary.ENTITY.save(newDictionary);

		String dictionaryId = VisEntitySkillDictionary.ENTITY.calculateId(key);
		CcpCacheDecorator cache = new CcpCacheDecorator(VisEntitySkillDictionary.ENTITY, dictionaryId);
		cache.delete();
		return json;
	}
}
