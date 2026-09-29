package com.vis.json.fields.validation;

import com.jn.utils.JnLanguage;

/**
 * Types accepted by the {@code type} field of the skill hierarchy fix suggestions, that is,
 * whether the user asks to associate ({@code add}) or dissociate ({@code remove}) a skill of their
 * resume with an implicit knowledge ({@code parent}). Each type also carries how it is written in the
 * messages sent to the user.
 */
public enum VisSkillFixHierarchyTypes {

	add("associação", "association"),
	remove("desassociação", "dissociation")
	;

	private final String portugueseDescription;

	private final String englishDescription;

	private VisSkillFixHierarchyTypes(String portugueseDescription, String englishDescription) {
		this.portugueseDescription = portugueseDescription;
		this.englishDescription = englishDescription;
	}

	/**
	 * How this type is written in a message in the given language. Languages without their own text
	 * get the English one.
	 */
	public String getDescription(JnLanguage language) {
		boolean isPortuguese = JnLanguage.portuguese == language;

		if(isPortuguese) {
			return this.portugueseDescription;
		}

		return this.englishDescription;
	}
}
