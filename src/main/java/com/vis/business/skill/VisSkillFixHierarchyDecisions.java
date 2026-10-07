package com.vis.business.skill;

import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * Decision taken by the support bot operator on an item of a skill hierarchy fix request. Each decision also
 * carries the title of its group of items in the email sent to the user, which lives in
 * {@link com.jn.entities.JnEntitySystemMessage}, one record per decision and language.
 */
public enum VisSkillFixHierarchyDecisions implements JnSystemMessage {

	/** The item was approved. */
	approved,
	/** The item was rejected. */
	rejected
	;

	/**
	 * Title of the group of items with this decision, in the given language. Languages without their own
	 * text get the English one.
	 */
	public String getTitle(JnLanguage language) {
		String title = this.getMessage(language);
		return title;
	}
}
