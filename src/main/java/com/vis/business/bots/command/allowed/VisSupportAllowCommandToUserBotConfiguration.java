package com.vis.business.bots.command.allowed;

import java.util.Arrays;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.business.bots.JnBotConfigurationSeeds;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityBotCommandStep;
import com.jn.entities.JnEntityBotCommandStepEndMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.business.bots.VisCommandNamesInPortuguese;
import com.vis.business.bots.VisSupportBotCommands;

/**
 * Configuration of {@code /allowCommandToUser <email>} in the support bot: the user stops being ignored (in every
 * command); when the user was not ignored, the session ends with a notice to the operator. Seeded by
 * {@code VisEntityCommandNotAllowedToUser}. Until 2026-10-15 it was seeded by the bot entities of jb.
 */
public final class VisSupportAllowCommandToUserBotConfiguration {

	private VisSupportAllowCommandToUserBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.allowCommandToUser.name();
		String email = "{" + JnJsonCommonsFields.email + "}";

		CcpJsonRepresentation portuguese = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.portuguese, "O usuário " + email + " não está sendo ignorado pelo suporte");
		CcpJsonRepresentation english = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.english, "The user " + email + " is not being ignored by the support");
		CcpJsonRepresentation spanish = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.spanish, "El usuario " + email + " no está siendo ignorado por el soporte");
		int userNotIgnored = VisSupportAllowCommandToUserStatus.userNotIgnored.asNumber();
		CcpJsonRepresentation userNotIgnoredFlow = JnEntityBotCommandStep.getStepFlow(userNotIgnored, "", portuguese, english, spanish);
		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportAllowCommandToUser.class, "", userNotIgnoredFlow);

		// the command ends telling the operator that the requests of the user, in every command, reach the support again
		String portugueseMessage = "O usuário " + email + " não é mais ignorado pelo suporte: as próximas solicitações dele, em todos os comandos, voltarão a chegar ao suporte.";
		String englishMessage = "The user " + email + " is no longer ignored by the support: their next requests, in every command, will reach the support again.";
		String spanishMessage = "El usuario " + email + " ya no es ignorado por el soporte: sus próximas solicitudes, en todos los comandos, volverán a llegar al soporte.";
		CcpJsonRepresentation portugueseEndMessage = JnEntityBotCommandStepEndMessage.getEndMessage(commandName, JnLanguage.portuguese, portugueseMessage);
		CcpJsonRepresentation englishEndMessage = JnEntityBotCommandStepEndMessage.getEndMessage(commandName, JnLanguage.english, englishMessage);
		CcpJsonRepresentation spanishEndMessage = JnEntityBotCommandStepEndMessage.getEndMessage(commandName, JnLanguage.spanish, spanishMessage);
		List<CcpJsonRepresentation> endMessages = Arrays.asList(portugueseEndMessage, englishEndMessage, spanishEndMessage);

		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName, JnJsonCommonsFields.email)
				.portugueseNames(VisCommandNamesInPortuguese.allowCommandToUser)
				.steps(step)
				.endMessages(endMessages);
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}

	/**
	 * The flow item of a step that refuses to review a user ignored by the support: the session ends and the operator is
	 * told how to stop ignoring the user.
	 * @param userNotAllowedStatus the status the engine of the step throws for an ignored user
	 * @param email the placeholder of the email in the session
	 * @return the flow item
	 */
	public static CcpJsonRepresentation getUserNotAllowedFlow(int userNotAllowedStatus, String email) {
		String allowCommandToUser = "/" + VisSupportBotCommands.allowCommandToUser + " " + email;
		String allowCommandToUserInPortuguese = "/" + VisCommandNamesInPortuguese.allowCommandToUser.getValue() + " " + email;
		CcpJsonRepresentation portugueseNotAllowed = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.portuguese, "O usuário " + email + " está sendo ignorado pelo suporte e as solicitações dele não são atendidas em nenhum comando. Para voltar a atendê-lo, use " + allowCommandToUserInPortuguese);
		CcpJsonRepresentation englishNotAllowed = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.english, "The user " + email + " is being ignored by the support and their requests are not reviewed in any command. To review them again, use " + allowCommandToUser);
		CcpJsonRepresentation spanishNotAllowed = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.spanish, "El usuario " + email + " está siendo ignorado por el soporte y sus solicitudes no se atienden en ningún comando. Para volver a atenderlo, use " + allowCommandToUser);
		CcpJsonRepresentation userNotAllowedFlow = JnEntityBotCommandStep.getStepFlow(userNotAllowedStatus, "", portugueseNotAllowed, englishNotAllowed, spanishNotAllowed);
		return userNotAllowedFlow;
	}
}
