package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * The key of a report of invalid LinkedIn in the counter of the tickets by kind ({@code counterKeyClass} of a
 * {@code @CcpEntityGrouperChildCounter} counted by {@code JnGrouperChildCounterEntityBeforeWrite}): always
 * {@code invalidLinkedin}.
 */
public class VisInvalidLinkedinTicketKindKeys implements Function<CcpJsonRepresentation, String>{

	/** The kind of the ticket. */
	public static enum Values implements CcpJsonFieldName{
		/** A report of invalid LinkedIn. */
		invalidLinkedin,
		;
	}

	/** The single instance: the class keeps no state. */
	public final static VisInvalidLinkedinTicketKindKeys INSTANCE = new VisInvalidLinkedinTicketKindKeys();

	private VisInvalidLinkedinTicketKindKeys() {}

	/**
	 * The kind of the ticket.
	 * @param json the report
	 * @return {@code invalidLinkedin}
	 */
	public String apply(CcpJsonRepresentation json) {
		String ticketKind = Values.invalidLinkedin.getValue();
		return ticketKind;
	}
}
