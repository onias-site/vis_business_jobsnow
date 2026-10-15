package com.vis.business.bots.skill.hierarchy;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Session fields of the {@code fixSkillHierarchy} command: {@code reviewItems} are the pending items of the request
 * ({@code type} and {@code skill}) in the order they are shown, and {@code itemIndex} is the item being decided in the
 * one by one review. The text each step sends to the operator is {@code botReply} of {@code JnJsonInstantMessengerFields}
 * (the end message and the flow messages of the steps are just {@code {botReply}}).
 */
public enum VisSupportSkillFixHierarchyFields implements CcpJsonFieldName{
	/** The {@code reviewItems} field. */
	reviewItems,
	/** The {@code itemIndex} field. */
	itemIndex
}
