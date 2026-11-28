package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.compatibility.VisResumeRecruiterFacts;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnEmailHtml;
import com.jn.messages.JnSendMessageToUser;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterResumes;
import com.vis.entities.VisEntityResume;
import com.vis.messages.VisMessages;

/**
 * The e-mail of resumes to a recruiter whose e-mail is in the text of a job posting of {@code /saveJobPosting} (since
 * 2026-11-26): the resumes found by the search of the job posting (the same ids of its link), the ones this recruiter may
 * see (active, not restricted, with a valid LinkedIn and not forbidding the company of the recruiter, as the screen of
 * the link shows them, and, since 2026-11-27, not marked as unwanted by the recruiter), in the e-mail of the resumes of a job posting ({@code VisNotifyRecruiterAboutResumesOfTheJobPosting},
 * since 2026-11-27; until then the usual e-mail of resumes to the recruiters): the same table of the usual e-mail, opened
 * by the text of the job posting, without "see all" and with the subject of the first line of the text. As the e-mail of a single resume of a position, the dates of each resume are its
 * insertion and now, and no view is read. The list goes to {@code vis_recruiter_resumes}, so the recruiter gets no other
 * e-mail of resumes on the day ({@code VisBusinessSendResumesToRecruiters} skips who has the list of today).
 */
public class VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting implements CcpBusiness {

	/** The input of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The record of the recruiter, as stored in {@code vis_recruiter}. */
		recruiter,
		/** The ids of the resumes found (the hashes of their e-mails). */
		resumeIds,
		/** The text of the job posting, which opens the e-mail and gives its subject (since 2026-11-27). */
		description,
		/** The output: whether the e-mail went. */
		sent,
	}

	/** The texts that are not messages. */
	static enum Constants implements CcpJsonFieldName {
		LINE_BREAK { public String getValue() { return "\\r?\\n"; } },
		EMPTY { public String getValue() { return ""; } },
		ELLIPSIS { public String getValue() { return "..."; } },
		OPENING_BRACE { public String getValue() { return "{"; } },
		CLOSING_BRACE { public String getValue() { return "}"; } },
		OPENING_PARENTHESIS { public String getValue() { return "("; } },
		CLOSING_PARENTHESIS { public String getValue() { return ")"; } },
	}

	/** The longest first line of the text the subject names. */
	static final int LONGEST_FIRST_LINE = 150;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting INSTANCE = new VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting();

	private VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting() {}

	/**
	 * Sends the e-mail.
	 * @param json the recruiter and the ids of the resumes
	 * @return {@code {"sent": <whether the e-mail went>}}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation recruiter = json.getInnerJson(Fields.recruiter);
		List<String> resumeIds = json.getAsStringList(Fields.resumeIds);
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		// the rows keep only what the rules between recruiter and resume let go (VisResumeRecruiterCompatibility)
		List<CcpJsonRepresentation> resumes = VisBusinessResumesOfThePositionFilters.getActiveResumes(resumeIds);
		boolean noResume = resumes.isEmpty();

		if(noResume) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.sent, false);
		}

		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		Map<String, long[]> resumeDates = getResumeDates(resumes);
		List<CcpJsonRepresentation> resumesOfTheEmail = VisBusinessSendResumesToRecruiters.getResumesOfTheEmail(resumes, rankings, resumeDates, Map.of());
		VisResumeRecruiterFacts facts = VisBusinessSendResumesToRecruiters.loadTheFacts(email, resumesOfTheEmail);
		List<CcpJsonRepresentation> rows = VisBusinessSendResumesToRecruiters.getRowsOfTheRecruiter(email, resumesOfTheEmail, Map.of(), Map.of(), facts);
		boolean noRow = rows.isEmpty();

		if(noRow) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.sent, false);
		}

		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		String visApiUrl = linkAddresses.getAsString(VisRecruiterResumesFields.visApiUrl);
		String description = json.getAsString(Fields.description);
		sendTheEmail(email, rows, description, frontendUrl, visApiUrl);
		return CcpOtherConstants.EMPTY_JSON.put(Fields.sent, true);
	}

	/**
	 * Writes the whole list of the recruiter in {@code vis_recruiter_resumes} (which keeps the other e-mails of resumes
	 * away for the day) and sends the e-mail of the job posting with the first
	 * {@value VisBusinessSendResumesToRecruiters#RESUMES_IN_THE_EMAIL}: the text of the job posting opens it, there is no
	 * paragraph "see all", and the subject counts the resumes the e-mail shows and names the first line of the text.
	 */
	private static void sendTheEmail(String email, List<CcpJsonRepresentation> rowsWithSubjectItems, String description, String frontendUrl, String visApiUrl) {
		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation rowWithSubjectItems : rowsWithSubjectItems) {
			CcpJsonRepresentation row = rowWithSubjectItems.removeFields(VisRecruiterResumesFields.subjectItems);
			rows.add(row);
		}
		CcpJsonRepresentation recruiterResumes = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityRecruiterResumes.Fields.email, email)
				.put(VisEntityRecruiterResumes.Fields.resumes, rows);
		VisEntityRecruiterResumes.ENTITY.save(recruiterResumes);

		int total = rows.size();
		int shown = Math.min(VisBusinessSendResumesToRecruiters.RESUMES_IN_THE_EMAIL, total);
		List<CcpJsonRepresentation> rowsOfTheEmail = rows.subList(0, shown);
		CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.resumes, rowsOfTheEmail)
				.put(JnJsonCommonsFields.email, email)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.language, VisBusinessSendResumesToRecruiters.LANGUAGE);
		CcpJsonRepresentation withTable = VisBusinessRenderResumesTable.INSTANCE.execute(tableInput);
		String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);

		String subject = getSubject(description, shown);
		String recruiterEmail = JnEmailHtml.escape(email);
		String jobPosting = JnEmailHtml.escape(description);
		String unsubscribeLink = VisBusinessSendResumesToRecruiters.getUnsubscribeLink(email, frontendUrl);
		CcpJsonRepresentation emailHeaders = VisBusinessSendResumesToRecruiters.getEmailHeaders(email, visApiUrl);

		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(JnJsonCommonsFields.language, VisBusinessSendResumesToRecruiters.LANGUAGE)
				.put(VisRecruiterResumesFields.resumesTable, resumesTable)
				.put(VisRecruiterResumesFields.recruiterEmail, recruiterEmail)
				.put(VisRecruiterResumesFields.jobPosting, jobPosting)
				.put(VisRecruiterResumesFields.jobPostingSubject, subject)
				.put(VisRecruiterResumesFields.unsubscribeLink, unsubscribeLink)
				.put(VisRecruiterResumesFields.totalOfResumes, shown)
				.put(JnJsonCommonsFields.emailHeaders, emailHeaders);
		String topic = VisMessages.VisNotifyRecruiterAboutResumesOfTheJobPosting.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.email};
		new JnSendMessageToUser().sendAllMessages(message, topic, messageTypes, JnMessageSenderExceptionHandler.LENIENT);
	}

	/**
	 * The subject of the e-mail: "{n} currículos encontrados para o anuncio de vaga {first line}", or "currículo encontrado
	 * para o anuncio de vaga {first line}" when the e-mail shows a single resume.
	 * @param description the text of the job posting
	 * @param shown how many resumes the e-mail shows
	 * @return the subject
	 */
	static String getSubject(String description, int shown) {
		String firstLine = getFirstLine(description);
		CcpJsonRepresentation subjectValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.total, shown)
				.put(VisRecruiterResumesFields.firstLine, firstLine);
		VisRecruiterResumesTexts subjectText = shown > 1 ? VisRecruiterResumesTexts.jobPostingSubjectMany : VisRecruiterResumesTexts.jobPostingSubjectOne;
		String subject = subjectText.getMessage(VisBusinessSendResumesToRecruiters.LANGUAGE, subjectValues);
		return subject;
	}

	/**
	 * The first line of the text that is not blank, trimmed, cut at {@value #LONGEST_FIRST_LINE} characters (a text
	 * without line breaks would become a subject of thousands of characters) and with its braces turned into parentheses
	 * (the subject is still a template: a {@code {x}} of the text would be filled by the sending).
	 * @param description the text of the job posting
	 * @return the first line
	 */
	static String getFirstLine(String description) {
		String lineBreak = Constants.LINE_BREAK.getValue();
		String[] lines = description.split(lineBreak);
		String firstLine = Constants.EMPTY.getValue();
		for (String line : lines) {
			String trimmed = line.trim();
			boolean blank = trimmed.isEmpty();

			if(blank) {
				continue;
			}

			firstLine = trimmed;
			break;
		}
		boolean tooLong = firstLine.length() > LONGEST_FIRST_LINE;
		String cut = tooLong ? firstLine.substring(0, LONGEST_FIRST_LINE).trim() + Constants.ELLIPSIS.getValue() : firstLine;
		String withoutOpeningBrace = cut.replace(Constants.OPENING_BRACE.getValue(), Constants.OPENING_PARENTHESIS.getValue());
		String withoutClosingBrace = withoutOpeningBrace.replace(Constants.CLOSING_BRACE.getValue(), Constants.CLOSING_PARENTHESIS.getValue());
		return withoutClosingBrace;
	}

	/**
	 * The insertion of each resume and now, by the hash of its e-mail.
	 * @param resumes the stored resumes
	 * @return the dates
	 */
	private static Map<String, long[]> getResumeDates(List<CcpJsonRepresentation> resumes) {
		long now = System.currentTimeMillis();
		Map<String, long[]> resumeDates = new HashMap<>();
		for (CcpJsonRepresentation resume : resumes) {
			String emailHash = resume.getAsString(VisEntityResume.Fields.email);
			long insertion = VisResumesOfThePositionSender.getInsertion(resume, now);
			resumeDates.put(emailHash, new long[] {insertion, now});
		}
		return resumeDates;
	}
}
