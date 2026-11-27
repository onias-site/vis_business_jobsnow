package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.List;

import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * The texts of the hourly publishing of the job postings of {@code /saveJobPosting} in the groups of technology of the
 * user bot (since 2026-11-26), html of Telegram: one record of {@code jn_system_message} per item and language, seeded by
 * {@code VisEntityJobPosting}. The {@code {...}} are filled by {@link VisBusinessJobPostingPublishingMessage}.
 */
public enum VisJobPostingPublishingTexts implements JnSystemMessage {
	/** Above the text of the job posting. */
	header("<b>[AJUDE AS PESSOAS ENCAMINHANDO ESTA VAGA AO MÁXIMO DE GRUPOS E PESSOAS QUE VOCÊ PUDER 🙏🙏🙏]</b>",
			"<b>[HELP PEOPLE BY FORWARDING THIS JOB POSTING TO AS MANY GROUPS AND PEOPLE AS YOU CAN 🙏🙏🙏]</b>"),
	/** To the candidates, below the text. */
	candidates("<b>Envie seu curriculo a 2.000 recrutadores, receba vagas aderentes ao teu perfil, basta <a href=\"{frontendUrl}/meus-dados/sobre-mim\">clicar aqui</a></b>",
			"<b>Send your resume to 2,000 recruiters and receive job postings that fit your profile, just <a href=\"{frontendUrl}/meus-dados/sobre-mim\">click here</a></b>"),
	/** To the recruiters, when the job posting found resumes. */
	recruitersWithTheResumes("<b>Recrutador, cadastre vagas e receba currículos exatamente aderentes à sua vaga.\nEsta vaga por exemplo encontrou {resumesCount} currículos (média CLT {avgClt}, média PJ {avgPj}, média de experiência {avgExperience} anos) <a href=\"{linkToShowResumes}\">Ver quais são estes currículos</a></b>",
			"<b>Recruiter, register job postings and receive resumes that fit exactly your job posting.\nThis job posting, for example, found {resumesCount} resumes (average CLT {avgClt}, average contractor {avgPj}, average experience {avgExperience} years) <a href=\"{linkToShowResumes}\">See these resumes</a></b>"),
	/** To the recruiters, when the job posting found no resume (no link to show them). */
	recruiters("<b>Recrutador, cadastre vagas e receba currículos exatamente aderentes à sua vaga.</b>",
			"<b>Recruiter, register job postings and receive resumes that fit exactly your job posting.</b>"),
	/** An average that no resume found informed. */
	notInformed("não informada", "not informed"),
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
	private VisJobPostingPublishingTexts(String portuguese, String english) {
		this.portuguese = portuguese;
		this.english = english;
	}

	/**
	 * The seeds of every text, in Portuguese and in English.
	 * @return the records of {@code jn_system_message}
	 */
	public static List<CcpBulkItem> getSeeds() {
		List<CcpBulkItem> seeds = new ArrayList<>();
		VisJobPostingPublishingTexts[] texts = values();
		for (VisJobPostingPublishingTexts text : texts) {
			List<CcpBulkItem> portugueseSeed = text.toBulkItems(JnLanguage.portuguese, text.portuguese);
			List<CcpBulkItem> englishSeed = text.toBulkItems(JnLanguage.english, text.english);
			seeds.addAll(portugueseSeed);
			seeds.addAll(englishSeed);
		}
		return seeds;
	}
}
