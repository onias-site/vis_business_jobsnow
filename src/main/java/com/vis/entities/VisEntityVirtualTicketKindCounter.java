package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Virtual entity whose id identifies the groups of {@code JnEntityGrouper} that count the tickets of vis ever opened, by
 * kind (the options of the combo of the screen of the tickets of the user): the requests of association and dissociation
 * of hierarchy ({@link VisEntitySkillFixHierarchyPending}), the suggestions of skill ({@link VisEntitySkillPending}) and
 * the reports of invalid LinkedIn ({@link VisEntityInvalidLinkedinReport}), one group per entity
 * ({@code @CcpEntityGrouperChildCounter} with {@code JnGrouperChildCounterEntityBeforeWrite}, a history: the answer does
 * not subtract). They join the tickets of the contact us in the chart of tickets of the screen Curiosities (since
 * 2026-11-17). Its only field always holds {@code ticketKind}. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualTicketKindCounter.Fields.class)
public class VisEntityVirtualTicketKindCounter {

	/** The entity {@code vis_virtual_ticket_kind_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualTicketKindCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code ticketKind}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/** The name of the counting. */
	public static enum Values implements CcpJsonFieldName{
		/** What is counted: the kind of the ticket. */
		ticketKind,
		;
	}

	/**
	 * The combination of the groups: {@code {"counter": "ticketKind"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, Values.ticketKind);
		return combination;
	}
}
