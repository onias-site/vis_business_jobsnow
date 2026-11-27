package com.vis.business.bots.job.posting;

import com.jn.messages.JnSystemMessage;

/**
 * Texts of the {@code saveJobPosting} command, stored in {@code JnEntitySystemMessage} and seeded by
 * {@code VisEntityJobPosting} (through {@code VisSupportSaveJobPostingBotConfiguration}).
 */
public enum VisSupportSaveJobPostingMessages implements JnSystemMessage {
	/** Asks the operator for the text of the job posting. */
	askTheText,
	/** The text does not have from 100 to 5,000 characters: takes {@code {length}}. */
	invalidText,
	/** The text was received and goes to be saved, with the search of its resumes; asks for the next one or {@code /sair}. */
	saved,
	;
}
