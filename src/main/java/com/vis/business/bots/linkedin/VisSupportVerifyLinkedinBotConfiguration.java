package com.vis.business.bots.linkedin;

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
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Configuration of {@code /verifyLinkedinAddress <linkedinAddress> <resumeEmailSha1> <recruiterEmail>} in the support bot
 * (since 2026-11-12): a recruiter reported, on the page of the resume, that its LinkedIn address does not open; the
 * operator answers whether the recruiter is right. Seeded by {@code VisEntityResumeMessage}.
 */
public final class VisSupportVerifyLinkedinBotConfiguration {

	private VisSupportVerifyLinkedinBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.verifyLinkedinAddress.name();
		String decide = VisSupportVerifyLinkedinSteps.verifyLinkedinAddressDecide.name();
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		// a resume that no longer exists ends the session with that notice
		int resumeNotFound = VisSupportVerifyLinkedinStatus.resumeNotFound.asNumber();
		CcpJsonRepresentation resumeNotFoundFlow = JnEntityBotCommandStep.getStepFlow(resumeNotFound, "", botReply);
		CcpJsonRepresentation showStep = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportVerifyLinkedinShow.class, decide, resumeNotFoundFlow);

		// "Sim" or "Não" close the ticket; an answer not understood repeats the step
		int invalidAnswer = VisSupportVerifyLinkedinStatus.invalidAnswer.asNumber();
		int linkedinInvalid = VisSupportVerifyLinkedinStatus.linkedinInvalid.asNumber();
		int linkedinValid = VisSupportVerifyLinkedinStatus.linkedinValid.asNumber();
		CcpJsonRepresentation invalidAnswerFlow = JnEntityBotCommandStep.getStepFlow(invalidAnswer, decide, botReply);
		CcpJsonRepresentation linkedinInvalidFlow = JnEntityBotCommandStep.getStepFlow(linkedinInvalid, "", botReply);
		CcpJsonRepresentation linkedinValidFlow = JnEntityBotCommandStep.getStepFlow(linkedinValid, "", botReply);
		CcpJsonRepresentation decideStep = JnEntityBotCommandStep.getBotCommandStep(decide, VisSupportVerifyLinkedinDecide.class, "", invalidAnswerFlow, linkedinInvalidFlow, linkedinValidFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, decide);
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName, VisJsonCommonsFields.linkedinAddress, VisSupportVerifyLinkedinFields.resumeEmailSha1, VisSupportVerifyLinkedinFields.recruiterEmail)
				.portugueseNames(VisCommandNamesInPortuguese.verifyLinkedinAddress)
				.steps(showStep, decideStep)
				.endMessages(endMessages)
				.commandExplanation(commandName, JnLanguage.portuguese, "Quando um recrutador reporta que o endereço de LinkedIn de um currículo não abre, o operador de suporte evoca este comando para dizer se o recrutador tem razão")
				.commandExplanation(commandName, JnLanguage.english, "When a recruiter reports that the LinkedIn address of a resume does not open, the support operator invokes this command to tell whether the recruiter is right")
				.systemMessage(VisSupportVerifyLinkedinMessages.ticket
						, "LinkedIn reportado como inválido\n\n"
						+ "O recrutador {recruiterEmail} reportou que o endereço de LinkedIn {linkedinAddress} do currículo {resumeEmailSha1} não abre.\n\n"
						+ "O recrutador tem razão em dizer que o endereço do LinkedIn está inválido?\n\n"
						+ "{options}"
						, "LinkedIn reported as invalid\n\n"
						+ "The recruiter {recruiterEmail} reported that the LinkedIn address {linkedinAddress} of the resume {resumeEmailSha1} does not open.\n\n"
						+ "Is the recruiter right in saying that the LinkedIn address is invalid?\n\n"
						+ "{options}")
				.systemMessage(VisSupportVerifyLinkedinMessages.options
						, "Digite \"Sim\" ou \"Não\""
						, "Type \"Yes\" or \"No\"")
				.systemMessage(VisSupportVerifyLinkedinMessages.notUnderstood
						, "Não entendi a resposta.\n\n{options}"
						, "I did not understand the answer.\n\n{options}")
				.systemMessage(VisSupportVerifyLinkedinMessages.confirmedInvalid
						, "O endereço {linkedinAddress} foi marcado como inválido: o currículo não vai mais aos recrutadores, a visualização do recrutador foi apagada e o dono do currículo, se tiver chat, foi avisado."
						, "The address {linkedinAddress} was marked as invalid: the resume no longer goes to the recruiters, the view of the recruiter was deleted and the owner of the resume, if they have a chat, was told.")
				.systemMessage(VisSupportVerifyLinkedinMessages.confirmedValid
						, "O recrutador {recruiterEmail} foi avisado por e-mail de que o perfil {linkedinAddress} existe, e este currículo não vai mais para ele."
						, "The recruiter {recruiterEmail} was told by e-mail that the profile {linkedinAddress} exists, and this resume no longer goes to them.")
				.systemMessage(VisSupportVerifyLinkedinMessages.resumeNotFound
						, "Não existe mais o currículo {resumeEmailSha1}."
						, "The resume {resumeEmailSha1} no longer exists.")
				.systemMessage(VisSupportVerifyLinkedinMessages.viewComment
						, "O endereço de linkedin deste currículo não pôde ser aberto por este recrutador"
						, "The LinkedIn address of this resume could not be opened by this recruiter");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}
}
