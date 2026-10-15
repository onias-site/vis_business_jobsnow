package com.vis.business.bots.skill.suggestion;

import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.business.bots.JnBotConfigurationSeeds;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityBotCommandStep;
import com.jn.entities.JnEntityBotCommandStepEndMessage;
import com.jn.utils.JnLanguage;
import com.vis.business.bots.VisCommandNamesInPortuguese;
import com.vis.business.bots.VisSupportBotCommands;
import com.vis.business.bots.command.allowed.VisSupportAllowCommandToUserBotConfiguration;
import com.vis.entities.VisEntitySkillPending;

/**
 * Configuration of {@code /reviewSkillSuggestion <email> <skill>} in the support bot: shows the suggestion, the operator
 * approves, rejects or ignores the user (with a confirmation). Seeded by {@code VisEntitySkillPending}. Until 2026-10-15
 * it was seeded by the bot entities of jb.
 */
public final class VisSupportSkillSuggestionBotConfiguration {

	private VisSupportSkillSuggestionBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.reviewSkillSuggestion.name();
		String decide = VisSupportSkillSuggestionSteps.reviewSkillSuggestionDecide.name();
		String confirmIgnore = VisSupportSkillSuggestionSteps.reviewSkillSuggestionConfirmIgnore.name();

		CcpJsonRepresentation showStep = getShowStep(commandName, decide);
		CcpJsonRepresentation decideStep = getDecideStep(decide, confirmIgnore);
		CcpJsonRepresentation confirmIgnoreStep = getConfirmIgnoreStep(decide, confirmIgnore);
		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, decide, confirmIgnore);

		// the skill goes last because it may have spaces (the last parameter takes the rest of the text)
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName, VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill)
				.portugueseNames(VisCommandNamesInPortuguese.reviewSkillSuggestion)
				.steps(showStep, decideStep, confirmIgnoreStep)
				.endMessages(endMessages)
				.systemMessage(VisSupportSkillSuggestionMessages.request
						, "Sugestão de habilidade de {email}\n\n"
						+ "Habilidade: {skill}\n"
						+ "Sinônimos: {synonymNames}\n"
						+ "Justificativa do usuário: {description}\n\n"
						+ "{options}"
						, "Skill suggestion from {email}\n\n"
						+ "Skill: {skill}\n"
						+ "Synonyms: {synonymNames}\n"
						+ "User's justification: {description}\n\n"
						+ "{options}")
				.systemMessage(VisSupportSkillSuggestionMessages.options
						, "Responda com uma das opções:\n"
						+ "• aprovar <justificativa> — aprova a habilidade, que passa a ser reconhecida nos currículos\n"
						+ "• rejeitar <justificativa> — rejeita a habilidade\n"
						+ "• ignorar — descarta a sugestão e ignora as próximas deste usuário em todos os comandos\n"
						+ "A justificativa vai para o usuário e precisa ter de 10 a 500 caracteres."
						, "Answer with one of the options:\n"
						+ "• approve <justification> — approves the skill, which starts being recognized in the resumes\n"
						+ "• reject <justification> — rejects the skill\n"
						+ "• ignore — discards the suggestion and ignores the next ones of this user in every command\n"
						+ "The justification goes to the user and needs 10 to 500 characters.")
				.systemMessage(VisSupportSkillSuggestionMessages.notUnderstood
						, "Não entendi a resposta. Toda decisão precisa vir acompanhada de uma justificativa de 10 a 500 caracteres.\n\n{options}"
						, "I did not understand the answer. Every decision needs to come with a justification of 10 to 500 characters.\n\n{options}")
				.systemMessage(VisSupportSkillSuggestionMessages.approved
						, "A habilidade {skill} sugerida por {email} foi aprovada. O usuário será avisado por e-mail."
						, "The skill {skill} suggested by {email} was approved. The user will be notified by email.")
				.systemMessage(VisSupportSkillSuggestionMessages.rejected
						, "A habilidade {skill} sugerida por {email} foi rejeitada. O usuário será avisado por e-mail."
						, "The skill {skill} suggested by {email} was rejected. The user will be notified by email.")
				.systemMessage(VisSupportSkillSuggestionMessages.ignoreConfirmation
						, "Confirma que o usuário {email} será ignorado pelo suporte, em todos os comandos? "
						+ "Esta sugestão será descartada sem aviso ao usuário e as próximas não chegarão mais a você.\n"
						+ "Responda: sim ou não"
						, "Do you confirm that the user {email} will be ignored by the support, in every command? "
						+ "This suggestion will be discarded without notifying the user and the next ones will no longer reach you.\n"
						+ "Answer: yes or no")
				.systemMessage(VisSupportSkillSuggestionMessages.ignoreConfirmationNotUnderstood
						, "Não entendi a resposta.\n\n{ignoreConfirmation}"
						, "I did not understand the answer.\n\n{ignoreConfirmation}")
				.systemMessage(VisSupportSkillSuggestionMessages.ignoreCanceled
						, "O usuário não será ignorado.\n\n{options}"
						, "The user will not be ignored.\n\n{options}")
				.systemMessage(VisSupportSkillSuggestionMessages.userIgnored
						, "O usuário {email} foi ignorado pelo suporte, em todos os comandos. A sugestão da habilidade {skill} foi descartada."
						, "The user {email} was ignored by the support, in every command. The suggestion of the skill {skill} was discarded.");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}

	/**
	 * Shows the suggestion and goes on to the decision. Without a pending suggestion the session ends with a notice to
	 * the operator; a user ignored by the support is not reviewed, and the operator is told how to stop ignoring them.
	 */
	private static CcpJsonRepresentation getShowStep(String commandName, String decide) {
		String email = "{" + VisEntitySkillPending.Fields.email + "}";
		String skill = "{" + VisEntitySkillPending.Fields.skill + "}";

		CcpJsonRepresentation portuguese = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.portuguese, "Não há sugestão pendente da habilidade '" + skill + "' para o e-mail '" + email + "'");
		CcpJsonRepresentation english = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.english, "There is no pending suggestion of the skill '" + skill + "' for the e-mail '" + email + "'");
		CcpJsonRepresentation spanish = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.spanish, "No hay sugerencia pendiente de la habilidad '" + skill + "' para el correo '" + email + "'");
		int requestNotFound = VisSupportSkillSuggestionStatus.requestNotFound.asNumber();
		CcpJsonRepresentation requestNotFoundFlow = JnEntityBotCommandStep.getStepFlow(requestNotFound, "", portuguese, english, spanish);

		int userNotAllowed = VisSupportSkillSuggestionStatus.userNotAllowed.asNumber();
		CcpJsonRepresentation userNotAllowedFlow = VisSupportAllowCommandToUserBotConfiguration.getUserNotAllowedFlow(userNotAllowed, email);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportSkillSuggestionShowRequest.class, decide, requestNotFoundFlow, userNotAllowedFlow);
		return step;
	}

	/**
	 * Approve or reject (finishing the review) or ignore the user (going on to its confirmation). An answer not
	 * understood repeats this step; a suggestion withdrawn meanwhile ends the session.
	 */
	private static CcpJsonRepresentation getDecideStep(String decide, String confirmIgnore) {
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();
		String email = "{" + VisEntitySkillPending.Fields.email + "}";
		String skill = "{" + VisEntitySkillPending.Fields.skill + "}";

		CcpJsonRepresentation portuguese = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.portuguese, "A sugestão da habilidade '" + skill + "' do e-mail '" + email + "' não está mais pendente: o usuário desistiu dela.");
		CcpJsonRepresentation english = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.english, "The suggestion of the skill '" + skill + "' from the e-mail '" + email + "' is no longer pending: the user withdrew it.");
		CcpJsonRepresentation spanish = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.spanish, "La sugerencia de la habilidad '" + skill + "' del correo '" + email + "' ya no está pendiente: el usuario desistió de ella.");
		int requestNotFound = VisSupportSkillSuggestionStatus.requestNotFound.asNumber();
		CcpJsonRepresentation requestNotFoundFlow = JnEntityBotCommandStep.getStepFlow(requestNotFound, "", portuguese, english, spanish);

		int invalidAnswer = VisSupportSkillSuggestionStatus.invalidAnswer.asNumber();
		int reviewFinished = VisSupportSkillSuggestionStatus.reviewFinished.asNumber();
		int ignoreConfirmationAsked = VisSupportSkillSuggestionStatus.ignoreConfirmationAsked.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, decide, botReply);
		CcpJsonRepresentation reviewFinishedFlow = JnEntityBotCommandStep.getStepFlow(reviewFinished, "", botReply);
		CcpJsonRepresentation ignoreConfirmationAskedFlow = JnEntityBotCommandStep.getStepFlow(ignoreConfirmationAsked, confirmIgnore, botReply);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(decide, VisSupportSkillSuggestionDecide.class, "", invalidAnswerFlow, reviewFinishedFlow, ignoreConfirmationAskedFlow, requestNotFoundFlow);
		return step;
	}

	/**
	 * Confirmation of the intention to ignore the user: yes ends the session with the user ignored, no goes back to the
	 * decision, and an answer not understood repeats this step.
	 */
	private static CcpJsonRepresentation getConfirmIgnoreStep(String decide, String confirmIgnore) {
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		int invalidAnswer = VisSupportSkillSuggestionStatus.invalidAnswer.asNumber();
		int userIgnored = VisSupportSkillSuggestionStatus.userIgnored.asNumber();
		int ignoreCanceled = VisSupportSkillSuggestionStatus.ignoreCanceled.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, confirmIgnore, botReply);
		CcpJsonRepresentation userIgnoredFlow = JnEntityBotCommandStep.getStepFlow(userIgnored, "", botReply);
		CcpJsonRepresentation ignoreCanceledFlow = JnEntityBotCommandStep.getStepFlow(ignoreCanceled, decide, botReply);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(confirmIgnore, VisSupportSkillSuggestionConfirmIgnore.class, "", invalidAnswerFlow, userIgnoredFlow, ignoreCanceledFlow);
		return step;
	}
}
