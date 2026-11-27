package com.vis.business.bots.job.posting;

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

/**
 * Configuration of {@code /saveJobPosting} in the support bot (since 2026-11-26): the bot asks the operator for the text
 * of a job posting, from 100 to 5,000 characters, and saves it in {@code vis_job_posting}, whose queue finds the resumes
 * that best answer it; a text of another size is asked again. Seeded by {@code VisEntityJobPosting}.
 */
public final class VisSupportSaveJobPostingBotConfiguration {

	private VisSupportSaveJobPostingBotConfiguration() {}

	/**
	 * The seed records of the command.
	 * @return the bulk items
	 */
	public static List<CcpBulkItem> getSeeds() {
		String commandName = VisSupportBotCommands.saveJobPosting.name();
		String saveTheText = VisSupportSaveJobPostingSteps.saveJobPostingText.name();
		CcpJsonRepresentation[] botReply = JnEntityBotCommandStep.getBotReplyInEveryLanguage();

		// the question goes on to the text; a text of another size repeats the step of the text. A step needs a flow: the
		// question never diverts, so it gets the same one, which leads where the question already goes
		int invalidText = VisSupportSaveJobPostingStatus.invalidText.asNumber();
		CcpJsonRepresentation invalidTextFlow = JnEntityBotCommandStep.getStepFlow(invalidText, saveTheText, botReply);
		CcpJsonRepresentation askStep = JnEntityBotCommandStep.getBotCommandStep(commandName, VisSupportSaveJobPostingAskTheText.class, saveTheText, invalidTextFlow);
		CcpJsonRepresentation saveStep = JnEntityBotCommandStep.getBotCommandStep(saveTheText, VisSupportSaveJobPostingSaveTheText.class, "", invalidTextFlow);

		List<CcpJsonRepresentation> endMessages = JnEntityBotCommandStepEndMessage.getBotReplyEndMessages(commandName, saveTheText);
		JnBotConfigurationSeeds seeds = new JnBotConfigurationSeeds(JnMessageType.JnBotType.support)
				.command(commandName)
				.portugueseNames(VisCommandNamesInPortuguese.saveJobPosting)
				.steps(askStep, saveStep)
				.endMessages(endMessages)
				.commandExplanation(commandName, JnLanguage.portuguese, "Salva o texto de uma vaga e busca os currículos que atendem ao maior número possível das habilidades dela")
				.commandExplanation(commandName, JnLanguage.english, "Saves the text of a job posting and searches the resumes that answer as many of its skills as possible")
				.systemMessage(VisSupportSaveJobPostingMessages.askTheText
						, "Digite o texto da vaga, de 100 a 5000 caracteres."
						, "Type the text of the job posting, from 100 to 5000 characters.")
				.systemMessage(VisSupportSaveJobPostingMessages.invalidText
						, "O texto tem {length} caracteres. Digite o texto da vaga com 100 a 5000 caracteres."
						, "The text has {length} characters. Type the text of the job posting with 100 to 5000 characters.")
				.systemMessage(VisSupportSaveJobPostingMessages.saved
						, "A vaga foi recebida. Em instantes ela será gravada, junto com os currículos que atendem ao maior número possível das habilidades dela."
						, "The job posting was received. In a moment it will be saved, with the resumes that answer as many of its skills as possible.");
		List<CcpBulkItem> bulkItems = seeds.getBulkItems();
		return bulkItems;
	}
}
