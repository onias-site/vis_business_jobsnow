package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.business.messages.JnMessageType;
import com.vis.entities.VisEntityPositionInstantMessageGroup;

/**
 * The bot that publishes the position in the groups of instant messages, in the child of the groups of the days of
 * publishing (since 2026-11-24): the bot of the users, the one of the groups of positions
 * ({@code vis_position_instant_message_group}).
 */
public class VisPositionPublishingBotChildField implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	/** The single instance: the class keeps no state. */
	public final static VisPositionPublishingBotChildField INSTANCE = new VisPositionPublishingBotChildField();

	private VisPositionPublishingBotChildField() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation position) {
		String botType = JnMessageType.JnBotType.user.name();
		CcpJsonRepresentation childField = CcpOtherConstants.EMPTY_JSON.put(VisEntityPositionInstantMessageGroup.Fields.botType, botType);
		return childField;
	}
}