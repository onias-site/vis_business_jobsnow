package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpNumberDecorator;
import com.jn.utils.JnLanguage;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.entities.VisEntityJobPosting;

/**
 * The message of a job posting of {@code /saveJobPosting} in the groups of technology (since 2026-11-26), html of
 * Telegram ({@link VisJobPostingPublishingTexts}): the header, the text of the job posting (escaped), the paragraph to the
 * candidates and the one to the recruiters, this one with the count, the averages and the link of the resumes found when
 * there are resumes. Up to {@value #TELEGRAM_LIMIT} characters it is one piece; longer (the text reaches 5,000), the header
 * and the text go in as many pieces as the limit asks, cut at a blank, and the two paragraphs in the last one, sent as
 * answers to the first.
 */
public class VisBusinessJobPostingPublishingMessage implements CcpBusiness {

	/** The output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The pieces of the message, each one a message of its own. */
		pieces,
	}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		LINE_BREAK { public String getValue() { return "\n"; } },
		PARAGRAPH { public String getValue() { return "\n\n"; } },
		BLANK { public String getValue() { return " "; } },
		AMPERSAND { public String getValue() { return "&"; } },
		AMPERSAND_ESCAPED { public String getValue() { return "&amp;"; } },
		LESS_THAN { public String getValue() { return "<"; } },
		LESS_THAN_ESCAPED { public String getValue() { return "&lt;"; } },
		GREATER_THAN { public String getValue() { return ">"; } },
		GREATER_THAN_ESCAPED { public String getValue() { return "&gt;"; } },
		SEMICOLON { public String getValue() { return ";"; } },
	}

	/** The fields of the template of the recruiters. */
	static enum TemplateFields implements CcpJsonFieldName {
		resumesCount,
		avgClt,
		avgPj,
		avgExperience,
		linkToShowResumes,
	}

	/** The most characters of a message of Telegram. */
	public static final int TELEGRAM_LIMIT = 4096;

	/** The language of the groups. */
	static final JnLanguage LANGUAGE = JnLanguage.portuguese;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessJobPostingPublishingMessage INSTANCE = new VisBusinessJobPostingPublishingMessage();

	private VisBusinessJobPostingPublishingMessage() {}

	/**
	 * The pieces of the message of the job posting.
	 * @param jobPosting the record of {@code vis_job_posting}
	 * @return {@code {"pieces": [...]}}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jobPosting) {
		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		CcpJsonRepresentation linkValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
		String header = VisJobPostingPublishingTexts.header.getMessage(LANGUAGE);
		String description = jobPosting.getAsString(VisEntityJobPosting.Fields.description);
		String escapedDescription = escape(description);
		String candidates = VisJobPostingPublishingTexts.candidates.getMessage(LANGUAGE, linkValues);
		String recruiters = getRecruiters(jobPosting);
		String lineBreak = Constants.LINE_BREAK.getValue();
		String paragraph = Constants.PARAGRAPH.getValue();
		String body = header + lineBreak + escapedDescription;
		String footer = candidates + paragraph + recruiters;
		String whole = body + paragraph + footer;
		boolean fitsInOne = whole.length() <= TELEGRAM_LIMIT;

		if(fitsInOne) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.pieces, List.of(whole));
		}

		List<String> pieces = split(body);
		pieces.add(footer);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.pieces, pieces);
		return response;
	}

	/**
	 * The paragraph to the recruiters: with the count, the averages and the link when the job posting found resumes.
	 * @param jobPosting the record
	 * @return the paragraph
	 */
	private static String getRecruiters(CcpJsonRepresentation jobPosting) {
		// the search that writes the link writes the count too
		boolean noLink = false == jobPosting.containsAllFields(VisEntityJobPosting.Fields.linkToShowResumes);

		if(noLink) {
			return VisJobPostingPublishingTexts.recruiters.getMessage(LANGUAGE);
		}

		Integer resumesCount = jobPosting.getAsIntegerNumber(VisEntityJobPosting.Fields.resumesCount);

		if(resumesCount == 0) {
			return VisJobPostingPublishingTexts.recruiters.getMessage(LANGUAGE);
		}

		String link = jobPosting.getAsString(VisEntityJobPosting.Fields.linkToShowResumes);
		String avgClt = getCurrency(jobPosting, VisEntityJobPosting.Fields.avgClt);
		String avgPj = getCurrency(jobPosting, VisEntityJobPosting.Fields.avgPj);
		String avgExperience = getExperience(jobPosting);
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON
				.put(TemplateFields.resumesCount, resumesCount)
				.put(TemplateFields.avgClt, avgClt)
				.put(TemplateFields.avgPj, avgPj)
				.put(TemplateFields.avgExperience, avgExperience)
				.put(TemplateFields.linkToShowResumes, link);
		String recruiters = VisJobPostingPublishingTexts.recruitersWithTheResumes.getMessage(LANGUAGE, values);
		return recruiters;
	}

	/**
	 * An average of claim as Brazilian money ({@code R$ 9.000,00}), or "não informada".
	 * @param jobPosting the record
	 * @param field the average
	 * @return the text of the average
	 */
	private static String getCurrency(CcpJsonRepresentation jobPosting, CcpJsonFieldName field) {
		boolean notInformed = false == jobPosting.containsAllFields(field);

		if(notInformed) {
			return VisJobPostingPublishingTexts.notInformed.getMessage(LANGUAGE);
		}

		String average = jobPosting.getAsString(field);
		CcpNumberDecorator number = new CcpNumberDecorator(average);
		String currency = number.toBrazilianCurrency();
		return currency;
	}

	/**
	 * The average years of experience with one decimal ({@code 4,5}), or "não informada".
	 * @param jobPosting the record
	 * @return the text of the average
	 */
	private static String getExperience(CcpJsonRepresentation jobPosting) {
		boolean notInformed = false == jobPosting.containsAllFields(VisEntityJobPosting.Fields.avgExperience);

		if(notInformed) {
			return VisJobPostingPublishingTexts.notInformed.getMessage(LANGUAGE);
		}

		String average = jobPosting.getAsString(VisEntityJobPosting.Fields.avgExperience);
		CcpNumberDecorator number = new CcpNumberDecorator(average);
		String experience = number.toBrazilianNumber(1);
		return experience;
	}

	/**
	 * Cuts the text in pieces of up to {@value #TELEGRAM_LIMIT} characters, each one ending before a blank.
	 * @param text the header and the escaped text of the job posting
	 * @return the pieces
	 */
	private static List<String> split(String text) {
		List<String> pieces = new ArrayList<>();
		String rest = text;
		while (rest.length() > TELEGRAM_LIMIT) {
			int cut = getCut(rest);
			String piece = rest.substring(0, cut);
			pieces.add(piece);
			String remaining = rest.substring(cut);
			rest = remaining.strip();
		}
		pieces.add(rest);
		return pieces;
	}

	/**
	 * Where a text longer than the limit is cut: at the last blank or line break of the second half of the limit; with
	 * none, at the limit, moved back to the start of an escape the limit would cut in half ({@code &amp;}).
	 * @param text the text longer than the limit
	 * @return the length of the piece
	 */
	private static int getCut(String text) {
		String window = text.substring(0, TELEGRAM_LIMIT);
		int lastBlank = window.lastIndexOf(Constants.BLANK.getValue());
		int lastLineBreak = window.lastIndexOf(Constants.LINE_BREAK.getValue());
		int lastSeparator = Math.max(lastBlank, lastLineBreak);
		boolean separatorInTheSecondHalf = lastSeparator > TELEGRAM_LIMIT / 2;

		if(separatorInTheSecondHalf) {
			return lastSeparator;
		}

		int lastAmpersand = window.lastIndexOf(Constants.AMPERSAND.getValue());
		boolean escapeCutInHalf = lastAmpersand > 0 && window.indexOf(Constants.SEMICOLON.getValue(), lastAmpersand) < 0;

		if(escapeCutInHalf) {
			return lastAmpersand;
		}

		return TELEGRAM_LIMIT;
	}

	/**
	 * What the operator typed cannot open a tag in the html of Telegram.
	 * @param text the text
	 * @return the escaped text
	 */
	private static String escape(String text) {
		String withoutAmpersand = text.replace(Constants.AMPERSAND.getValue(), Constants.AMPERSAND_ESCAPED.getValue());
		String withoutLessThan = withoutAmpersand.replace(Constants.LESS_THAN.getValue(), Constants.LESS_THAN_ESCAPED.getValue());
		String escaped = withoutLessThan.replace(Constants.GREATER_THAN.getValue(), Constants.GREATER_THAN_ESCAPED.getValue());
		return escaped;
	}
}
