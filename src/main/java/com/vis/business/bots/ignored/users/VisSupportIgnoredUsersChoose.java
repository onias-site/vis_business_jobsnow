package com.vis.business.bots.ignored.users;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.business.bots.VisSupportBotCommands;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;

/**
 * Second step of the {@code ignoredUsers} command, repeated until the operator chooses a user: {@code 1} stops
 * ignoring the user shown, {@code 2} shows the next one (after the last one, the first one again). Any other answer
 * shows the same user again ({@code invalidAnswer}).
 *
 * <p>Choosing the user ends this session ({@code userChosen}) and asks the bot to handle
 * {@code /allowCommandToUser <email>} as if the operator had typed it ({@link JnJsonInstantMessengerFields#typedValueToRedirect}),
 * with this command as the list to show again ({@link JnJsonInstantMessengerFields#listToShowAgain}), so that the remaining
 * ignored users are listed when it finishes. The list is read again at every answer: a user shown that was allowed in
 * the meantime is not allowed twice; the operator is told so and sees the first one.
 */
public class VisSupportIgnoredUsersChoose implements CcpBusiness {

	/** The answer that stops ignoring the user shown. */
	private static final String ALLOW = "1";

	/** The answer that shows the next user. */
	private static final String NEXT = "2";

	/**
	 * Handles the answer of the operator: stops ignoring the user shown (redirecting to {@code /allowCommandToUser}),
	 * shows the next one (going back to the first after the last), or asks again.
	 * @param json the session, with the user shown and the answer
	 * @return the session with the reply of the bot
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		List<CcpJsonRepresentation> ignoredUsers = VisSupportIgnoredUsersList.getIgnoredUsers();
		JnLanguage language = VisSupportIgnoredUsersList.getLanguage(json);
		boolean noIgnoredUser = ignoredUsers.isEmpty();

		if(noIgnoredUser) {
			CcpJsonRepresentation ended = VisSupportIgnoredUsersList.noIgnoredUser(json, language);
			return ended;
		}

		List<String> emails = VisSupportIgnoredUsersList.getEmails(ignoredUsers);
		String currentUser = json.getAsString(VisSupportIgnoredUsersFields.currentUser);
		int currentIndex = emails.indexOf(currentUser);
		boolean allowedInTheMeantime = currentIndex < 0;
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String answer = typedValue.trim();

		if(allowedInTheMeantime) {
			String alreadyAllowed = VisSupportIgnoredUsersList.getMessageAboutTheUser(language, VisSupportIgnoredUsersMessages.userAlreadyAllowed, currentUser);
			CcpJsonRepresentation showingTheFirst = this.showUser(json, language, ignoredUsers, 0, alreadyAllowed);
			return showingTheFirst;
		}

		boolean allow = ALLOW.equals(answer);

		if(allow) {
			String userChosen = VisSupportIgnoredUsersList.getMessageAboutTheUser(language, VisSupportIgnoredUsersMessages.userChosen, currentUser);
			String allowCommand = "/" + VisSupportBotCommands.allowCommandToUser + " " + currentUser;
			String thisCommand = "/" + VisSupportBotCommands.ignoredUsers;
			CcpJsonRepresentation jsonWithReply = json.put(JnJsonInstantMessengerFields.botReply, userChosen);
			CcpJsonRepresentation jsonWithTheCommand = jsonWithReply.put(JnJsonInstantMessengerFields.typedValueToRedirect, allowCommand);
			CcpJsonRepresentation jsonWithTheListToShowAgain = jsonWithTheCommand.put(JnJsonInstantMessengerFields.listToShowAgain, thisCommand);
			CcpJsonRepresentation chosen = VisSupportIgnoredUsersStatus.userChosen.throwException(jsonWithTheListToShowAgain);
			return chosen;
		}

		boolean next = NEXT.equals(answer);

		if(next) {
			int nextIndex = (currentIndex + 1) % ignoredUsers.size();
			CcpJsonRepresentation showingTheNext = this.showUser(json, language, ignoredUsers, nextIndex, "");
			return showingTheNext;
		}

		String notUnderstood = VisSupportIgnoredUsersMessages.notUnderstood.getMessage(language);
		CcpJsonRepresentation showingTheSame = this.showUser(json, language, ignoredUsers, currentIndex, notUnderstood);
		CcpJsonRepresentation askedAgain = VisSupportIgnoredUsersStatus.invalidAnswer.throwException(showingTheSame);
		return askedAgain;
	}

	/**
	 * Puts in the session the reply that shows the user at the position, after a notice.
	 * @param json the session
	 * @param language the language of the reply
	 * @param ignoredUsers the ignored users
	 * @param userIndex the position of the user
	 * @param notice the notice before the user, possibly empty
	 * @return the session with the reply and the user shown
	 */
	private CcpJsonRepresentation showUser(CcpJsonRepresentation json, JnLanguage language, List<CcpJsonRepresentation> ignoredUsers, int userIndex, String notice) {
		String userPrompt = VisSupportIgnoredUsersList.getUserPrompt(language, ignoredUsers, userIndex);
		String reply = notice + userPrompt;
		CcpJsonRepresentation ignoredUser = ignoredUsers.get(userIndex);
		String email = ignoredUser.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation jsonWithReply = json.put(JnJsonInstantMessengerFields.botReply, reply);
		CcpJsonRepresentation jsonWithTheUser = jsonWithReply.put(VisSupportIgnoredUsersFields.currentUser, email);
		return jsonWithTheUser;
	}
}
