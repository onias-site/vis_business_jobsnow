package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * The key of a request of hierarchy in the counter of the tickets by kind ({@code counterKeyClass} of a
 * {@code @CcpEntityGrouperChildCounter} counted by {@code JnGrouperChildCounterEntityBeforeWrite}): {@code hierarchyAdd}
 * or {@code hierarchyRemove}, by its {@code type} (part of the primary key, so always present).
 */
public class VisSkillFixHierarchyTicketKindKeys implements Function<CcpJsonRepresentation, String>{
	/** The single instance: the class keeps no state. */
	public final static VisSkillFixHierarchyTicketKindKeys INSTANCE = new VisSkillFixHierarchyTicketKindKeys();

	private VisSkillFixHierarchyTicketKindKeys() {}

	/**
	 * The kind of the ticket.
	 * @param json the request
	 * @return {@code hierarchyAdd} or {@code hierarchyRemove}
	 */
	public String apply(CcpJsonRepresentation json) {
		String typeName = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.type);
		VisSkillFixHierarchyTypes type = VisSkillFixHierarchyTypes.valueOf(typeName);
		String ticketKind = type.getTicketKind();
		return ticketKind;
	}
}
