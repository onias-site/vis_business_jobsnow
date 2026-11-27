package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.List;

import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * The text of the answer of the support bot to a job posting written in a group of Telegram (since 2026-11-27), html of
 * Telegram: one record of {@code jn_system_message} per item and language, seeded by
 * {@code VisEntityInstantMessagePositionReply}. The {@code {...}} are filled by
 * {@link VisBusinessReplyTheJobPostingWithItsResumes}; the averages of claim already come as money ({@code R$ 9.500,00}).
 */
public enum VisJobPostingReplyTexts implements JnSystemMessage {
	/** The resumes found by the job posting, as an answer to the message of the job posting. */
	resumesFound("Foram encontrados {resumesCount} currículos (média CLT {avgClt}, média PJ {avgPj}, média de experiência {avgExperience} anos)\n<a href=\"{linkToShowResumes}\">Ver quais são estes currículos</a>",
			"{resumesCount} resumes were found (average CLT {avgClt}, average contractor {avgPj}, average experience {avgExperience} years)\n<a href=\"{linkToShowResumes}\">See these resumes</a>"),
	;

	/** The text in Portuguese. */
	private final String portuguese;

	/** The text in English. */
	private final String english;

	/**
	 * Keeps the texts of the item.
	 * @param portuguese the text in Portuguese
	 * @param english the text in English
	 */
	private VisJobPostingReplyTexts(String portuguese, String english) {
		this.portuguese = portuguese;
		this.english = english;
	}

	/**
	 * The seeds of every text, in Portuguese and in English.
	 * @return the records of {@code jn_system_message}
	 */
	public static List<CcpBulkItem> getSeeds() {
		List<CcpBulkItem> seeds = new ArrayList<>();
		VisJobPostingReplyTexts[] texts = values();
		for (VisJobPostingReplyTexts text : texts) {
			List<CcpBulkItem> portugueseSeed = text.toBulkItems(JnLanguage.portuguese, text.portuguese);
			List<CcpBulkItem> englishSeed = text.toBulkItems(JnLanguage.english, text.english);
			seeds.addAll(portugueseSeed);
			seeds.addAll(englishSeed);
		}
		return seeds;
	}
}
