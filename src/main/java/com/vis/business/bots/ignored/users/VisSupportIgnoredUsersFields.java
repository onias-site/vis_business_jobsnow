package com.vis.business.bots.ignored.users;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Session fields of the {@code ignoredUsers} command: {@code currentUser} is the email of the user being shown. The
 * email, and not its position, is what the session keeps: the list is read again at every answer, and the users the
 * operator stopped ignoring meanwhile are no longer in it. The text each step sends to the operator is {@code botReply}
 * of {@code JnJsonInstantMessengerFields} (the end message and the flow messages of the steps are just {@code {botReply}}).
 */
public enum VisSupportIgnoredUsersFields implements CcpJsonFieldName{
	/** The {@code currentUser} field. */
	currentUser
}
