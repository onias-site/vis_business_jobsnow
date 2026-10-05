package com.vis.json.fields.validation;

import com.jn.entities.JnEntitySystemMessage;
import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * Types accepted by the {@code type} field of the skill hierarchy fix suggestions, that is,
 * whether the user asks to associate ({@code add}) or dissociate ({@code remove}) a skill of their
 * resume with an implicit knowledge ({@code parent}). How each type is written in the messages sent
 * to the user lives in {@link JnEntitySystemMessage}, one record per type and language.
 */
public enum VisSkillFixHierarchyTypes implements JnSystemMessage {

	/** Associate the skill with the implicit knowledge. */
	add,
	/** Dissociate the skill from the implicit knowledge. */
	remove
	;

	/**
	 * How this type is written in a message in the given language.
	 */
	public String getDescription(JnLanguage language) {
		String description = this.getMessage(language);
		return description;
	}
}
