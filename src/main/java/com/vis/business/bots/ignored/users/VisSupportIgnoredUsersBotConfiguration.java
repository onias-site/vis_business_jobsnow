package com.vis.business.bots.ignored.users;

import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.business.bots.JnBotConfigurationSeeds;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityBotCommandStep;
import com.jn.entities.JnEntityBotCommandStepEndMessage;
import com.vis.business.bots.VisCommandNamesInPortuguese;
import com.vis.business.bots.VisSupportBotCommands;

/**
 * Configuration of {@code /ignoredUsers} in the support bot: shows how many users the support is ignoring and the first
 * one, and goes on to the choice between stopping ignoring them (the bot then runs {@code /allowCommandToUser} for the
 * user and shows the list again) and going to the next one. Seeded by {@code VisEntityCommandNotAllowedToUser}. Until
 * 2026-10-15 it was seeded by the bot entities of jb.
 */
public final class VisSupportIgnoredUsersBotConfiguration {

	private VisSupportIgnoredUsersBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.ignoredUsers.name();
		String choose = VisSupportIgnoredUsersSteps.ignoredUsersChoose.name();
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		// without any ignored user the session ends with that notice
		int noIgnoredUser = VisSupportIgnoredUsersStatus.noIgnoredUser.asNumber();
		CcpJsonRepresentation noIgnoredUserFlowOfTheList = JnEntityBotCommandStep.getStepFlow(noIgnoredUser, "", botReply);
		CcpJsonRepresentation showStep = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportIgnoredUsersShow.class, choose, noIgnoredUserFlowOfTheList);

		// stop ignoring the user shown or go to the next one; an answer not understood repeats the step and a list that
		// became empty in the meantime ends the session
		int invalidAnswer = VisSupportIgnoredUsersStatus.invalidAnswer.asNumber();
		int userChosen = VisSupportIgnoredUsersStatus.userChosen.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, choose, botReply);
		CcpJsonRepresentation noIgnoredUserFlow = JnEntityBotCommandStep.getStepFlow(noIgnoredUser, "", botReply);
		CcpJsonRepresentation userChosenFlow = JnEntityBotCommandStep.getStepFlow(userChosen, "", botReply);
		CcpJsonRepresentation chooseStep = JnEntityBotCommandStep.getBotCommandStep(choose, VisSupportIgnoredUsersChoose.class, choose, invalidAnswerFlow, noIgnoredUserFlow, userChosenFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, choose);

		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName)
				.portugueseNames(VisCommandNamesInPortuguese.ignoredUsers)
				.steps(showStep, chooseStep)
				.endMessages(endMessages)
				.systemMessage(VisSupportIgnoredUsersMessages.oneUser
						, "O suporte está ignorando 1 usuário"
						, "The support is ignoring 1 user")
				.systemMessage(VisSupportIgnoredUsersMessages.manyUsers
						, "O suporte está ignorando {usersCount} usuários"
						, "The support is ignoring {usersCount} users")
				.systemMessage(VisSupportIgnoredUsersMessages.userPrompt
						, "Usuário {userNumber} de {usersCount} => {email}\n"
						+ "Ignorado no comando {commandName} ({request})\n"
						+ "Digite \"1\" para voltar a atendê-lo ou \"2\" para ir ao próximo"
						, "User {userNumber} of {usersCount} => {email}\n"
						+ "Ignored in the command {commandName} ({request})\n"
						+ "Type \"1\" to review their requests again or \"2\" to go to the next one")
				.systemMessage(VisSupportIgnoredUsersMessages.noIgnoredUser
						, "O suporte não está ignorando nenhum usuário."
						, "The support is not ignoring any user.")
				.systemMessage(VisSupportIgnoredUsersMessages.notUnderstood
						, "Não entendi a resposta.\n\n"
						, "I did not understand the answer.\n\n")
				.systemMessage(VisSupportIgnoredUsersMessages.userAlreadyAllowed
						, "O usuário {email} já não é mais ignorado.\n\n"
						, "The user {email} is no longer ignored.\n\n")
				.systemMessage(VisSupportIgnoredUsersMessages.userChosen
						, "Voltando a atender o usuário {email}"
						, "Reviewing the requests of the user {email} again");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}
}
