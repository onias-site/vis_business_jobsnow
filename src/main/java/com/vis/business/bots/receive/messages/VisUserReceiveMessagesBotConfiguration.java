package com.vis.business.bots.receive.messages;

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
import com.vis.business.bots.VisUserBotCommands;

/**
 * Configuration of {@code /receiveMessages <email>} in the user bot (since 2026-11-12): the first step asks the login
 * token of the e-mail ({@link VisUserReceiveMessagesAskToken}), or ends the command when the e-mail is already registered
 * with this chat; the second checks it and saves the chat of the e-mail in {@code jn_instant_messenger_user}
 * ({@link VisUserReceiveMessagesCheckToken}), or waits for another token while the chat has attempts. Seeded by
 * {@code VisEntityLoginTokenAttempts}.
 */
public final class VisUserReceiveMessagesBotConfiguration {

	private VisUserReceiveMessagesBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisUserBotCommands.receiveMessages.name();
		String tokenStep = VisUserReceiveMessagesSteps.receiveMessagesToken.name();
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		// a blocked chat, an invalid e-mail or an e-mail already registered with this chat end the session with that notice
		int blocked = VisUserReceiveMessagesStatus.blocked.asNumber();
		int invalidEmail = VisUserReceiveMessagesStatus.invalidEmail.asNumber();
		int alreadyRegistered = VisUserReceiveMessagesStatus.alreadyRegistered.asNumber();
		CcpJsonRepresentation blockedFlow = JnEntityBotCommandStep.getStepFlow(blocked, "", botReply);
		CcpJsonRepresentation invalidEmailFlow = JnEntityBotCommandStep.getStepFlow(invalidEmail, "", botReply);
		CcpJsonRepresentation alreadyRegisteredFlow = JnEntityBotCommandStep.getStepFlow(alreadyRegistered, "", botReply);
		CcpJsonRepresentation askTokenStep = JnEntityBotCommandStep.getBotCommandStep(commandName, VisUserReceiveMessagesAskToken.class, tokenStep, blockedFlow, invalidEmailFlow, alreadyRegisteredFlow);

		// a wrong token waits for another one; at the limit, or without resume, the session ends
		int wrongToken = VisUserReceiveMessagesStatus.wrongToken.asNumber();
		int resumeNotFound = VisUserReceiveMessagesStatus.resumeNotFound.asNumber();
		CcpJsonRepresentation wrongTokenFlow = JnEntityBotCommandStep.getStepFlow(wrongToken, tokenStep, botReply);
		CcpJsonRepresentation resumeNotFoundFlow = JnEntityBotCommandStep.getStepFlow(resumeNotFound, "", botReply);
		CcpJsonRepresentation checkTokenStep = JnEntityBotCommandStep.getBotCommandStep(tokenStep, VisUserReceiveMessagesCheckToken.class, "", wrongTokenFlow, blockedFlow, resumeNotFoundFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, tokenStep);
		String email = "{" + JnJsonCommonsFields.email + "}";
		String remainingAttempts = "{" + VisUserReceiveMessagesConversation.Fields.remainingAttempts + "}";
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.user)
				.command(commandName, JnJsonCommonsFields.email)
				.portugueseNames(VisCommandNamesInPortuguese.receiveMessages)
				.steps(askTokenStep, checkTokenStep)
				.endMessages(endMessages)
				.commandExplanation(commandName, JnLanguage.portuguese, "Passa a enviar por aqui as mensagens sobre o currículo do e-mail informado, depois de você digitar o token de login que enviamos a esse e-mail. Uso: /receiveMessages seu@email.com")
				.commandExplanation(commandName, JnLanguage.english, "Starts sending here the messages about the resume of the e-mail informed, after you type the login token we sent to that e-mail. Usage: /receiveMessages your@email.com")
				.systemMessage(VisUserReceiveMessagesMessages.askToken
						, "Digite o token de login que enviamos ao e-mail " + email + "."
						, "Type the login token we sent to the e-mail " + email + ".")
				.systemMessage(VisUserReceiveMessagesMessages.invalidEmail
						, "O e-mail '" + email + "' informado é inválido. Uso: /receiveMessages seu@email.com"
						, "The e-mail '" + email + "' is invalid. Usage: /receiveMessages your@email.com")
				.systemMessage(VisUserReceiveMessagesMessages.wrongToken
						, "O token informado está errado. Restam " + remainingAttempts + " tentativa(s). Se você não recebeu ou perdeu o token original, use o comando /resendLoginToken " + email
						, "The token is wrong. You have " + remainingAttempts + " attempt(s) left. If you did not receive or lost the original token, use the command /resendLoginToken " + email)
				.systemMessage(VisUserReceiveMessagesMessages.blocked
						, "Você excedeu o número de tentativas e está bloqueado: não poderá mais usar o comando /receiveMessages por este bot."
						, "You exceeded the number of attempts and are blocked: you can no longer use the command /receiveMessages through this bot.")
				.systemMessage(VisUserReceiveMessagesMessages.resumeNotFound
						, "Não encontramos currículo cadastrado para o e-mail " + email + ". Cadastre o seu currículo no Onias.Site e depois use de novo o comando /receiveMessages " + email
						, "We found no resume registered for the e-mail " + email + ". Register your resume on Onias.Site and then use the command /receiveMessages " + email + " again")
				.systemMessage(VisUserReceiveMessagesMessages.chatSaved
						, "Pronto! A partir de agora você receberá por aqui as mensagens do Onias.Site sobre o seu currículo."
						, "Done! From now on you will receive here the messages of Onias.Site about your resume.")
				.systemMessage(VisUserReceiveMessagesMessages.alreadyRegistered
						, "O e-mail " + email + " já está cadastrado para receber por aqui as mensagens do Onias.Site sobre o seu currículo. Não é preciso fazer mais nada."
						, "The e-mail " + email + " is already registered to receive here the messages of Onias.Site about your resume. There is nothing else to do.");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}
}
