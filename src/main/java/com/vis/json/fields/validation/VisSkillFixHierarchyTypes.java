package com.vis.json.fields.validation;

import java.util.Set;

import com.ccp.decorators.CcpJsonFieldName;
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
	add {
		public void execute(Set<String> parents, String parent) {
			parents.add(parent);
		}

		public String getTicketKind() {
			return TicketKinds.hierarchyAdd.getValue();
		}
	},
	/** Dissociate the skill from the implicit knowledge. */
	remove {
		public void execute(Set<String> parents, String parent) {
			parents.remove(parent);

		}

		public String getTicketKind() {
			return TicketKinds.hierarchyRemove.getValue();
		}
	}
	;

	/** The kinds of ticket of the requests of hierarchy (the options of the combo of the screen of the tickets of the user). */
	public static enum TicketKinds implements CcpJsonFieldName{
		/** A request of association. */
		hierarchyAdd,
		/** A request of dissociation. */
		hierarchyRemove,
		;
	}

	public abstract void execute(Set<String> parents, String parent);

	/**
	 * The kind of ticket of a request of this type, its key in the counter of the tickets by kind (screen Curiosities).
	 * @return {@code hierarchyAdd} or {@code hierarchyRemove}
	 */
	public abstract String getTicketKind();

	/**
	 * How this type is written in a message in the given language.
	 */
	public String getDescription(JnLanguage language) {
		String description = this.getMessage(language);
		return description;
	}
}
