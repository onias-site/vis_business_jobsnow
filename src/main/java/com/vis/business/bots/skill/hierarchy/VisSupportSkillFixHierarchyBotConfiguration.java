package com.vis.business.bots.skill.hierarchy;

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
import com.vis.business.skill.VisSkillFixHierarchyDecisions;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Configuration of {@code /fixSkillHierarchy <type> <email> <parent>} in the support bot: shows the request, the operator
 * approves all, rejects all, decides item by item or ignores the user (with a confirmation). Seeded by
 * {@code VisEntitySkillFixHierarchyPending}. Until 2026-10-15 it was seeded by the bot entities of jb.
 */
public final class VisSupportSkillFixHierarchyBotConfiguration {

	private VisSupportSkillFixHierarchyBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.fixSkillHierarchy.name();
		String chooseMode = VisSupportSkillFixHierarchySteps.fixSkillHierarchyChooseMode.name();
		String decideItem = VisSupportSkillFixHierarchySteps.fixSkillHierarchyDecideItem.name();
		String confirmIgnore = VisSupportSkillFixHierarchySteps.fixSkillHierarchyConfirmIgnore.name();

		CcpJsonRepresentation showStep = getShowRequestStep(commandName, chooseMode);
		CcpJsonRepresentation chooseModeStep = getChooseModeStep(chooseMode, decideItem, confirmIgnore);
		CcpJsonRepresentation decideItemStep = getDecideItemStep(decideItem);
		CcpJsonRepresentation confirmIgnoreStep = getConfirmIgnoreStep(chooseMode, confirmIgnore);
		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, chooseMode, decideItem, confirmIgnore);

		// the operator reviews one type (add or remove) at a time; the parent goes last because it may have spaces (until
		// 2026-10-08 it went first and FRONT END was read as parent FRONT, type END)
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName, VisEntitySkillFixHierarchyPending.Fields.type, VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent)
				.portugueseNames(VisCommandNamesInPortuguese.fixSkillHierarchy)
				.steps(showStep, chooseModeStep, decideItemStep, confirmIgnoreStep)
				.endMessages(endMessages)
				.systemMessage(VisSkillFixHierarchyTypes.add, "associação", "association")
				.systemMessage(VisSkillFixHierarchyTypes.remove, "desassociação", "dissociation")
				.systemMessage(VisSkillFixHierarchyDecisions.approved, "Itens aprovados", "Approved items")
				.systemMessage(VisSkillFixHierarchyDecisions.rejected, "Itens reprovados", "Rejected items")
				.systemMessage(VisSupportSkillFixHierarchyMessages.options
						, "Responda com uma das opções:\n"
						+ "• aprovar <justificativa> — aprova todos os itens pendentes\n"
						+ "• rejeitar <justificativa> — rejeita todos os itens pendentes\n"
						+ "• um a um — decide item por item\n"
						+ "• ignorar — descarta a solicitação e ignora as próximas deste usuário em todos os comandos"
						, "Answer with one of the options:\n"
						+ "• approve <justification> — approves all the pending items\n"
						+ "• reject <justification> — rejects all the pending items\n"
						+ "• one by one — decides item by item\n"
						+ "• ignore — discards the request and ignores the next ones of this user in every command")
				.systemMessage(VisSupportSkillFixHierarchyMessages.ignoreConfirmation
						, "Confirma que o usuário {email} será ignorado pelo suporte, em todos os comandos? "
						+ "Esta solicitação será descartada sem aviso ao usuário e as próximas não chegarão mais a você.\n"
						+ "Responda: sim ou não"
						, "Do you confirm that the user {email} will be ignored by the support, in every command? "
						+ "This request will be discarded without notifying the user and the next ones will no longer reach you.\n"
						+ "Answer: yes or no")
				.systemMessage(VisSupportSkillFixHierarchyMessages.previousDecisionApproved
						, "item já aprovado em revisão anterior"
						, "item already approved in an earlier review")
				.systemMessage(VisSupportSkillFixHierarchyMessages.previousDecisionRejected
						, "item já reprovado em revisão anterior"
						, "item already rejected in an earlier review")
				.systemMessage(VisSupportSkillFixHierarchyMessages.notUnderstood
						, "Não entendi a resposta. Toda decisão precisa vir acompanhada de uma justificativa.\n\n"
						, "I did not understand the answer. Every decision needs to come with a justification.\n\n")
				.systemMessage(VisSupportSkillFixHierarchyMessages.itemPrompt
						, "Item {itemNumber} de {itemsCount}: {skill} ({typeDescription} com o termo {parent})\n"
						+ "Responda: aprovar <justificativa> ou rejeitar <justificativa>"
						, "Item {itemNumber} of {itemsCount}: {skill} ({typeDescription} with the term {parent})\n"
						+ "Answer: approve <justification> or reject <justification>")
				.systemMessage(VisSupportSkillFixHierarchyMessages.reviewFinished
						, "Revisão concluída para {email} / {parent}.\n"
						+ "Aprovados: {approvedSkills}\n"
						+ "Reprovados: {rejectedSkills}\n"
						+ "O usuário será avisado por e-mail."
						, "Review finished for {email} / {parent}.\n"
						+ "Approved: {approvedSkills}\n"
						+ "Rejected: {rejectedSkills}\n"
						+ "The user will be notified by email.")
				.systemMessage(VisSupportSkillFixHierarchyMessages.noSkill, "-", "-")
				.systemMessage(VisSupportSkillFixHierarchyMessages.userIgnored
						, "O usuário {email} foi ignorado pelo suporte, em todos os comandos. A solicitação para o termo {parent} foi descartada."
						, "The user {email} was ignored by the support, in every command. The request for the term {parent} was discarded.")
				.systemMessage(VisSupportSkillFixHierarchyMessages.ignoreCanceled
						, "O usuário não será ignorado.\n\n{options}"
						, "The user will not be ignored.\n\n{options}")
				.systemMessage(VisSupportSkillFixHierarchyMessages.ignoreConfirmationNotUnderstood
						, "Não entendi a resposta.\n\n{ignoreConfirmation}"
						, "I did not understand the answer.\n\n{ignoreConfirmation}")
				.systemMessage(VisSupportSkillFixHierarchyMessages.requestHeader
						, "Solicitação de {typeDescription} de {email} para o termo {parent}\n\n"
						, "Request of {typeDescription} from {email} for the term {parent}\n\n")
				.systemMessage(VisSupportSkillFixHierarchyMessages.requestTypeJustification
						, "[{typeDescription}]\nJustificativa do usuário: {description}\n"
						, "[{typeDescription}]\nUser's justification: {description}\n")
				.systemMessage(VisSupportSkillFixHierarchyMessages.pendingItems
						, "Itens pendentes: {skills}\n"
						, "Pending items: {skills}\n")
				.systemMessage(VisSupportSkillFixHierarchyMessages.approvedBefore
						, "Já aprovados anteriormente (não serão perguntados): {skills}\n"
						, "Already approved before (will not be asked): {skills}\n")
				.systemMessage(VisSupportSkillFixHierarchyMessages.rejectedBefore
						, "Já reprovados anteriormente (não serão perguntados): {skills}\n"
						, "Already rejected before (will not be asked): {skills}\n")
				.systemMessage(VisSupportSkillFixHierarchyMessages.requestWithdrawn
						, "O usuário {email} desistiu da solicitação de {typeDescription} com o termo {parent} enquanto ela era avaliada. "
						+ "A avaliação foi encerrada e as decisões tomadas até aqui foram descartadas."
						, "The user {email} withdrew the {typeDescription} request with the term {parent} while it was being reviewed. "
						+ "The review was ended and the decisions taken so far were discarded.");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}

	/**
	 * Shows the request and goes on to the choice of how to decide it. Without any item the session ends with a notice to
	 * the operator; when every item was decided in earlier reviews, the review finishes right away with its summary; a
	 * user ignored by the support is not reviewed, and the operator is told how to stop ignoring them.
	 */
	private static CcpJsonRepresentation getShowRequestStep(String commandName, String chooseMode) {
		String email = "{" + VisEntitySkillFixHierarchyPending.Fields.email + "}";
		String parent = "{" + VisEntitySkillFixHierarchyPending.Fields.parent + "}";
		String type = "{" + VisEntitySkillFixHierarchyPending.Fields.type + "}";

		CcpJsonRepresentation portuguese = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.portuguese, "Não há itens pendentes de ajuste (" + type + ") na hierarquia de conhecimentos para o e-mail '" + email + "' e o termo '" + parent + "'");
		CcpJsonRepresentation english = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.english, "There are no pending skill hierarchy fix items (" + type + ") for the e-mail '" + email + "' and the term '" + parent + "'");
		CcpJsonRepresentation spanish = JnEntityBotCommandStep.getStepFlowMessage(JnLanguage.spanish, "No hay ítems pendientes de ajuste (" + type + ") en la jerarquía de conocimientos para el correo '" + email + "' y el término '" + parent + "'");
		int requestNotFound = VisSupportSkillFixHierarchyStatus.requestNotFound.asNumber();
		CcpJsonRepresentation requestNotFoundFlow = JnEntityBotCommandStep.getStepFlow(requestNotFound, "", portuguese, english, spanish);

		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();
		int reviewFinished = VisSupportSkillFixHierarchyStatus.reviewFinished.asNumber();
		CcpJsonRepresentation reviewFinishedFlow = JnEntityBotCommandStep.getStepFlow(reviewFinished, "", botReply);

		int userNotAllowed = VisSupportSkillFixHierarchyStatus.userNotAllowed.asNumber();
		CcpJsonRepresentation userNotAllowedFlow = VisSupportAllowCommandToUserBotConfiguration.getUserNotAllowedFlow(userNotAllowed, email);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportSkillFixHierarchyShowRequest.class, chooseMode, requestNotFoundFlow, reviewFinishedFlow, userNotAllowedFlow);
		return step;
	}

	/**
	 * Approve all, reject all (finishing the review), one by one (going on to the item decision) or ignore the user (going
	 * on to its confirmation). An answer not understood repeats this step; a request the user withdrew meanwhile ends the
	 * session with a notice to the operator.
	 */
	private static CcpJsonRepresentation getChooseModeStep(String chooseMode, String decideItem, String confirmIgnore) {
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		int invalidAnswer = VisSupportSkillFixHierarchyStatus.invalidAnswer.asNumber();
		int reviewFinished = VisSupportSkillFixHierarchyStatus.reviewFinished.asNumber();
		int ignoreConfirmationAsked = VisSupportSkillFixHierarchyStatus.ignoreConfirmationAsked.asNumber();
		int requestWithdrawn = VisSupportSkillFixHierarchyStatus.requestWithdrawn.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, chooseMode, botReply);
		CcpJsonRepresentation reviewFinishedFlow = JnEntityBotCommandStep.getStepFlow(reviewFinished, "", botReply);
		CcpJsonRepresentation ignoreConfirmationAskedFlow = JnEntityBotCommandStep.getStepFlow(ignoreConfirmationAsked, confirmIgnore, botReply);
		CcpJsonRepresentation requestWithdrawnFlow = JnEntityBotCommandStep.getStepFlow(requestWithdrawn, "", botReply);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(chooseMode, VisSupportSkillFixHierarchyChooseMode.class, decideItem, invalidAnswerFlow, reviewFinishedFlow, ignoreConfirmationAskedFlow, requestWithdrawnFlow);
		return step;
	}

	/**
	 * Decision on one item; the step repeats itself until the last item, which finishes the review. A request the user
	 * withdrew in the middle of the review ends the session with a notice to the operator.
	 */
	private static CcpJsonRepresentation getDecideItemStep(String decideItem) {
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		int invalidAnswer = VisSupportSkillFixHierarchyStatus.invalidAnswer.asNumber();
		int reviewFinished = VisSupportSkillFixHierarchyStatus.reviewFinished.asNumber();
		int requestWithdrawn = VisSupportSkillFixHierarchyStatus.requestWithdrawn.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, decideItem, botReply);
		CcpJsonRepresentation reviewFinishedFlow = JnEntityBotCommandStep.getStepFlow(reviewFinished, "", botReply);
		CcpJsonRepresentation requestWithdrawnFlow = JnEntityBotCommandStep.getStepFlow(requestWithdrawn, "", botReply);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(decideItem, VisSupportSkillFixHierarchyDecideItem.class, decideItem, invalidAnswerFlow, reviewFinishedFlow, requestWithdrawnFlow);
		return step;
	}

	/**
	 * Confirmation of the intention to ignore the user: yes ends the session with the user ignored, no goes back to the
	 * choice of how to decide the items, and an answer not understood repeats this step.
	 */
	private static CcpJsonRepresentation getConfirmIgnoreStep(String chooseMode, String confirmIgnore) {
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		int invalidAnswer = VisSupportSkillFixHierarchyStatus.invalidAnswer.asNumber();
		int userIgnored = VisSupportSkillFixHierarchyStatus.userIgnored.asNumber();
		int ignoreCanceled = VisSupportSkillFixHierarchyStatus.ignoreCanceled.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, confirmIgnore, botReply);
		CcpJsonRepresentation userIgnoredFlow = JnEntityBotCommandStep.getStepFlow(userIgnored, "", botReply);
		CcpJsonRepresentation ignoreCanceledFlow = JnEntityBotCommandStep.getStepFlow(ignoreCanceled, chooseMode, botReply);

		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(confirmIgnore, VisSupportSkillFixHierarchyConfirmIgnore.class, "", invalidAnswerFlow, userIgnoredFlow, ignoreCanceledFlow);
		return step;
	}
}
