package com.vis.business.skill;

import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * How each {@link VisSkillFixHierarchyDecisions decision} is written beside an item in the {@code explanation} of a
 * reviewed skill hierarchy fix request, which the user reads in the "Motivo" panel of the screen. The texts live in
 * {@link com.jn.entities.JnEntitySystemMessage}, one record per decision and language.
 */
public enum VisSkillFixHierarchyDecisionNames implements JnSystemMessage {
	/** The item was approved. */
	approved,
	/** The item was rejected. */
	rejected
	;

	/**
	 * The name of the decision in the given language. Languages without their own text get the English one.
	 * @param language the language
	 * @return the name
	 */
	public String getName(JnLanguage language) {
		String name = this.getMessage(language);
		return name;
	}
}
