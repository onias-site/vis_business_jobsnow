package com.vis.business.skill;

import com.jn.utils.JnLanguage;

/**
 * Decision taken by the support bot operator on an item of a skill hierarchy fix request. Each decision also
 * carries the title of its group of items in the email sent to the user.
 */
public enum VisSkillFixHierarchyDecisions {

	approved("Itens aprovados", "Approved items"),
	rejected("Itens reprovados", "Rejected items")
	;

	private final String portugueseTitle;

	private final String englishTitle;

	private VisSkillFixHierarchyDecisions(String portugueseTitle, String englishTitle) {
		this.portugueseTitle = portugueseTitle;
		this.englishTitle = englishTitle;
	}

	/**
	 * Title of the group of items with this decision, in the given language. Languages without their own
	 * text get the English one.
	 */
	public String getTitle(JnLanguage language) {
		boolean isPortuguese = JnLanguage.portuguese == language;

		if(isPortuguese) {
			return this.portugueseTitle;
		}

		return this.englishTitle;
	}
}
