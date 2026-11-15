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
 * recruiter in {@code vis_recruiter}; an invalid address, a recruiter already active or an inactive one brought back end
 * the session with their own reply. Seeded by {@code VisEntityRecruiterSignUpReturn}.
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

		int invalidEmail = VisSupportSaveRecruiterStatus.invalidEmail.asNumber();
		int alreadyRecruiter = VisSupportSaveRecruiterStatus.alreadyRecruiter.asNumber();
		int reactivated = VisSupportSaveRecruiterStatus.reactivated.asNumber();
		CcpJsonRepresentation invalidEmailFlow = JnEntityBotCommandStep.getStepFlow(invalidEmail, "", botReply);
		CcpJsonRepresentation alreadyRecruiterFlow = JnEntityBotCommandStep.getStepFlow(alreadyRecruiter, "", botReply);
		CcpJsonRepresentation reactivatedFlow = JnEntityBotCommandStep.getStepFlow(reactivated, "", botReply);
		CcpJsonRepresentation step = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportSaveRecruiter.class, "", invalidEmailFlow, alreadyRecruiterFlow, reactivatedFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName);
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName, JnJsonCommonsFields.email)
				.portugueseNames(VisCommandNamesInPortuguese.saveRecruiter)
				.steps(step)
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
						, "O recrutador {email} estava inativo e foi reativado."
						, "The recruiter {email} was inactive and was reactivated.")
				.systemMessage(VisSupportSaveRecruiterMessages.saved
						, "O recrutador {email} foi cadastrado."
						, "The recruiter {email} was saved.");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}
}
