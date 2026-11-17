package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * The key of a suggestion of skill in the counter of the tickets by kind ({@code counterKeyClass} of a
 * {@code @CcpEntityGrouperChildCounter} counted by {@code JnGrouperChildCounterEntityBeforeWrite}): always
 * {@code skillSuggestion}.
 */
public class VisSkillPendingTicketKindKeys implements Function<CcpJsonRepresentation, String>{

	/** The kind of the ticket. */
	public static enum Values implements CcpJsonFieldName{
		/** A suggestion of skill. */
		skillSuggestion,
		;
	}

	/** The single instance: the class keeps no state. */
	public final static VisSkillPendingTicketKindKeys INSTANCE = new VisSkillPendingTicketKindKeys();

	private VisSkillPendingTicketKindKeys() {}

	/**
	 * The kind of the ticket.
	 * @param json the suggestion
	 * @return {@code skillSuggestion}
	 */
	public String apply(CcpJsonRepresentation json) {
		String ticketKind = Values.skillSuggestion.getValue();
		return ticketKind;
	}
}
