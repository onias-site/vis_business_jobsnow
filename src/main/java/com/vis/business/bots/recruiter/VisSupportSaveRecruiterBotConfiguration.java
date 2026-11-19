package com.vis.business.bots.recruiter;

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
 * Configuration of {@code /saveRecruiter <email>} in the support bot (since 2026-11-14): the operator saves an e-mail as a
 * recruiter in {@code vis_recruiter}; an invalid address ends the session with its own reply. A recruiter already there
 * is changed only after the operator confirms it (since 2026-11-19): an inactive one is brought back with the source
 * {@code support}; an active one is inactivated and recorded in {@code vis_recruiter_unsubscribed}. Seeded by
 * {@code VisEntityRecruiterSignUpReturn}.
 */
public final class VisSupportSaveRecruiterBotConfiguration {

	private VisSupportSaveRecruiterBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.saveRecruiter.name();
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		String confirmReactivation = VisSupportSaveRecruiterSteps.saveRecruiterConfirmReactivation.name();
		String confirmInactivation = VisSupportSaveRecruiterSteps.saveRecruiterConfirmInactivation.name();

		// a recruiter already there is not changed at once: the operator is asked to confirm the reactivation of an
		// inactive one or the inactivation of an active one (since 2026-11-19)
		int invalidEmail = VisSupportSaveRecruiterStatus.invalidEmail.asNumber();
		int alreadyRecruiter = VisSupportSaveRecruiterStatus.alreadyRecruiter.asNumber();
		int askReactivation = VisSupportSaveRecruiterStatus.askReactivation.asNumber();
		int askInactivation = VisSupportSaveRecruiterStatus.askInactivation.asNumber();
		CcpJsonRepresentation invalidEmailFlow = JnEntityBotCommandStep.getStepFlow(invalidEmail, "", botReply);
		CcpJsonRepresentation askReactivationFlow = JnEntityBotCommandStep.getStepFlow(askReactivation, confirmReactivation, botReply);
		CcpJsonRepresentation askInactivationFlow = JnEntityBotCommandStep.getStepFlow(askInactivation, confirmInactivation, botReply);
		CcpJsonRepresentation firstStep = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportSaveRecruiter.class, "", invalidEmailFlow, askReactivationFlow, askInactivationFlow);

		// "Sim" inactivates and records the unsubscription, "Não" writes nothing; an answer not understood repeats the step
		int inactivated = VisSupportSaveRecruiterStatus.inactivated.asNumber();
		int inactivationCancelled = VisSupportSaveRecruiterStatus.inactivationCancelled.asNumber();
		int noLongerActive = VisSupportSaveRecruiterStatus.noLongerActive.asNumber();
		int invalidAnswerOfTheInactivation = VisSupportSaveRecruiterStatus.invalidAnswer.asNumber();
		CcpJsonRepresentation inactivatedFlow = JnEntityBotCommandStep.getStepFlow(inactivated, "", botReply);
		CcpJsonRepresentation inactivationCancelledFlow = JnEntityBotCommandStep.getStepFlow(inactivationCancelled, "", botReply);
		CcpJsonRepresentation noLongerActiveFlow = JnEntityBotCommandStep.getStepFlow(noLongerActive, "", botReply);
		CcpJsonRepresentation invalidAnswerOfTheInactivationFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswerOfTheInactivation, confirmInactivation, botReply);
		CcpJsonRepresentation confirmInactivationStep = JnEntityBotCommandStep.getBotCommandStep(confirmInactivation, VisSupportSaveRecruiterConfirmInactivation.class, "", inactivatedFlow, inactivationCancelledFlow, noLongerActiveFlow, invalidAnswerOfTheInactivationFlow);

		// "Sim" reactivates with the source support, "Não" writes nothing; an answer not understood repeats the step
		int reactivated = VisSupportSaveRecruiterStatus.reactivated.asNumber();
		int reactivationCancelled = VisSupportSaveRecruiterStatus.reactivationCancelled.asNumber();
		int invalidAnswer = VisSupportSaveRecruiterStatus.invalidAnswer.asNumber();
		CcpJsonRepresentation reactivatedFlow = JnEntityBotCommandStep.getStepFlow(reactivated, "", botReply);
		CcpJsonRepresentation reactivationCancelledFlow = JnEntityBotCommandStep.getStepFlow(reactivationCancelled, "", botReply);
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, confirmReactivation, botReply);
		CcpJsonRepresentation alreadyRecruiterMeanwhileFlow = JnEntityBotCommandStep.getStepFlow(alreadyRecruiter, "", botReply);
		CcpJsonRepresentation confirmStep = JnEntityBotCommandStep.getBotCommandStep(confirmReactivation, VisSupportSaveRecruiterConfirmReactivation.class, "", reactivatedFlow, reactivationCancelledFlow, invalidAnswerFlow, alreadyRecruiterMeanwhileFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, confirmReactivation, confirmInactivation);
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName, JnJsonCommonsFields.email)
				.portugueseNames(VisCommandNamesInPortuguese.saveRecruiter)
				.steps(firstStep, confirmStep, confirmInactivationStep)
				.endMessages(endMessages)
				.commandExplanation(commandName, JnLanguage.portuguese, "Cadastra um e-mail como recrutador, para que ele passe a receber o e-mail diário de currículos")
				.commandExplanation(commandName, JnLanguage.english, "Saves an e-mail as a recruiter, so that it starts receiving the daily e-mail of resumes")
				.systemMessage(VisSupportSaveRecruiterMessages.invalidEmail
						, "O e-mail {email} não é válido. Nenhum recrutador foi cadastrado."
						, "The e-mail {email} is not valid. No recruiter was saved.")
				.systemMessage(VisSupportSaveRecruiterMessages.alreadyRecruiter
						, "O e-mail {email} já está cadastrado como recrutador."
						, "The e-mail {email} is already saved as a recruiter.")
				.systemMessage(VisSupportSaveRecruiterMessages.reactivated
						, "O recrutador {email} estava inativo e foi reativado, agora com a origem support."
						, "The recruiter {email} was inactive and was reactivated, now with the source support.")
				.systemMessage(VisSupportSaveRecruiterMessages.confirmReactivation
						, "O e-mail {email} é de um recrutador inativo. Quer mesmo reativar este recrutador? Se sim, a origem dele passa a ser support.\n\n{options}"
						, "The e-mail {email} belongs to an inactive recruiter. Do you really want to reactivate this recruiter? If so, their source becomes support.\n\n{options}")
				.systemMessage(VisSupportSaveRecruiterMessages.options
						, "Digite \"Sim\" ou \"Não\""
						, "Type \"Yes\" or \"No\"")
				.systemMessage(VisSupportSaveRecruiterMessages.notUnderstood
						, "Não entendi a resposta.\n\n{options}"
						, "I did not understand the answer.\n\n{options}")
				.systemMessage(VisSupportSaveRecruiterMessages.reactivationCancelled
						, "O recrutador {email} continua inativo. Nada foi gravado."
						, "The recruiter {email} stays inactive. Nothing was written.")
				.systemMessage(VisSupportSaveRecruiterMessages.confirmInactivation
						, "O e-mail {email} é de um recrutador ativo. Deseja inativar este recrutador? Se sim, ele também fica marcado como descadastrado e para de receber o e-mail de currículos.\n\n{options}"
						, "The e-mail {email} belongs to an active recruiter. Do you want to inactivate this recruiter? If so, they are also marked as unsubscribed and stop receiving the e-mail of resumes.\n\n{options}")
				.systemMessage(VisSupportSaveRecruiterMessages.inactivated
						, "O recrutador {email} foi inativado e marcado como descadastrado."
						, "The recruiter {email} was inactivated and marked as unsubscribed.")
				.systemMessage(VisSupportSaveRecruiterMessages.inactivationCancelled
						, "O recrutador {email} continua ativo. Nada foi gravado."
						, "The recruiter {email} stays active. Nothing was written.")
				.systemMessage(VisSupportSaveRecruiterMessages.noLongerActive
						, "O recrutador {email} já não está ativo. Nada foi gravado."
						, "The recruiter {email} is no longer active. Nothing was written.")
				.systemMessage(VisSupportSaveRecruiterMessages.saved
						, "O recrutador {email} foi cadastrado."
						, "The recruiter {email} was saved.");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}
}
