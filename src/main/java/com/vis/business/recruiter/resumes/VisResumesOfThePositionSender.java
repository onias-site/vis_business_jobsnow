package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.compatibility.VisResumeRecruiterFacts;
import com.vis.business.compatibility.VisResumeRecruiterFactsOfASavedResume;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.decorators.CcpUrlDecorator;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.messages.JnSendMessageToUser;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionResumes;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisPositionFrequencyTypes;
import com.vis.messages.VisMessages;

/**
 * Sends the recruiter of a position the e-mail with the resumes that match it (since 2026-11-24): a single resume for the
 * positions that receive them instantly ("Cv recebido para a vaga"), the resumes of the period for the others ("{n} Cvs
 * recebidos para a vaga"). The rows are the ones of the daily e-mail to every recruiter ({@code VisBusinessBuildResumeRow}),
 * for the e-mail that receives the resumes of the position, in the order chosen for the position; the e-mail shows the
 * first {@value VisBusinessSendResumesToRecruiters#RESUMES_IN_THE_EMAIL} and the whole list goes to
 * {@code vis_position_resumes}, opened by the link "see all" (the screen of the resumes of the position).
 * <p>
 * Receives the position and its resumes ({@code position}, {@code resumes}, the resumes as stored). The views, the aliases
 * and the dates of the resumes come in the constructor: a process that sends several positions reads them once.
 */
public class VisResumesOfThePositionSender implements Consumer<CcpJsonRepresentation> {

	/** The input of each sending. */
	public static enum Fields implements CcpJsonFieldName {
		/** The position, as stored. */
		position,
		/** The resumes that match the position, as stored. */
		resumes,
	}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		SEE_ALL_URL { public String getValue() { return "{frontendUrl}/meu-recrutamento/curriculos-da-vaga?email={email}&vaga={positionTitle}&mes={positionYearMonth}"; } },
		INSERTION_DATE_PATTERN { public String getValue() { return "dd/MM/yyyy HH:mm:ss"; } },
		NO_SAVED_RESUME { public String getValue() { return ""; } },
	}

	/** The oldest (added) and the newest (last change) times of each resume, by the hash of its e-mail. */
	private final Map<String, long[]> resumeDates;

	/** How many recruiters viewed each resume, by the hash of its LinkedIn. */
	private final Map<String, Long> viewsByResume;

	/** The last view of each pair recruiter + resume. */
	private final Map<String, Long> lastViewByPair;

	/** The alias of each pair recruiter + resume. */
	private final Map<String, String> aliasByPair;

	/** The e-mail (hashed) of the resume just saved, whose save released its restriction; empty for a period. */
	private final String savedResumeEmailHash;

	/** The address of the front end. */
	private final String frontendUrl;

	/**
	 * Keeps what the e-mails need.
	 * @param resumeDates the oldest and the newest times of each resume
	 * @param viewsByResume how many recruiters viewed each resume
	 * @param lastViewByPair the last view of each pair recruiter + resume
	 * @param aliasByPair the alias of each pair recruiter + resume
	 * @param frontendUrl the address of the front end
	 */
	public VisResumesOfThePositionSender(Map<String, long[]> resumeDates, Map<String, Long> viewsByResume, Map<String, Long> lastViewByPair,
			Map<String, String> aliasByPair, String frontendUrl) {
		this(resumeDates, viewsByResume, lastViewByPair, aliasByPair, Constants.NO_SAVED_RESUME.getValue(), frontendUrl);
	}

	private VisResumesOfThePositionSender(Map<String, long[]> resumeDates, Map<String, Long> viewsByResume, Map<String, Long> lastViewByPair,
			Map<String, String> aliasByPair, String savedResumeEmailHash, String frontendUrl) {
		this.resumeDates = resumeDates;
		this.viewsByResume = viewsByResume;
		this.lastViewByPair = lastViewByPair;
		this.aliasByPair = aliasByPair;
		this.savedResumeEmailHash = savedResumeEmailHash;
		this.frontendUrl = frontendUrl;
	}

	/**
	 * A sender for a single resume just saved: no views or aliases (reading their whole history at each save of a resume
	 * would cost too much), the resume added at its insertion and changed now.
	 * @param resume the resume, as stored
	 * @return the sender
	 */
	public static VisResumesOfThePositionSender ofASavedResume(CcpJsonRepresentation resume) {
		String emailHash = resume.getAsString(VisEntityResume.Fields.email);
		long now = System.currentTimeMillis();
		long insertion = getInsertion(resume, now);
		Map<String, long[]> resumeDates = new HashMap<>();
		resumeDates.put(emailHash, new long[] {insertion, now});
		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		VisResumesOfThePositionSender sender = new VisResumesOfThePositionSender(resumeDates, Map.of(), Map.of(), Map.of(), emailHash, frontendUrl);
		return sender;
	}

	// the insertionDate is a text (dd/MM/yyyy HH:mm:ss); without it, the resume was inserted now
	static long getInsertion(CcpJsonRepresentation resume, long now) {
		boolean noInsertionDate = false == resume.containsAllFields(VisEntityResume.Fields.insertionDate);
		if(noInsertionDate) {
			return now;
		}
		String insertionDate = resume.getAsString(VisEntityResume.Fields.insertionDate);
		String pattern = Constants.INSERTION_DATE_PATTERN.getValue();
		CcpTimeDecorator insertion = new CcpTimeDecorator(insertionDate, pattern);
		Long insertionTime = insertion.getContent();
		return insertionTime;
	}

	public void accept(CcpJsonRepresentation json) {
		CcpJsonRepresentation position = json.getInnerJson(Fields.position);
		List<CcpJsonRepresentation> matchingResumes = json.getAsJsonList(Fields.resumes);
		boolean noResume = matchingResumes.isEmpty();
		if(noResume) {
			return;
		}
		String email = position.getAsString(VisEntityPosition.Fields.resumesEmail);
		// the rows keep only the resumes the rules between recruiter and resume let reach this e-mail
		// (VisResumeRecruiterCompatibility): nothing goes to who unsubscribed
		List<CcpJsonRepresentation> rows = this.getRows(email, matchingResumes);
		boolean noRow = rows.isEmpty();
		if(noRow) {
			return;
		}
		CcpJsonRepresentation sortInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessSortResumesOfThePosition.Fields.position, position)
				.put(VisBusinessSortResumesOfThePosition.Fields.resumes, rows);
		CcpJsonRepresentation sorted = VisBusinessSortResumesOfThePosition.INSTANCE.execute(sortInput);
		List<CcpJsonRepresentation> sortedRows = sorted.getAsJsonList(VisBusinessSortResumesOfThePosition.Fields.resumes);
		this.sendTheEmail(position, email, sortedRows);
	}

	// the rows of the daily e-mail for the e-mail of the position, each one with the values of the order of the position
	private List<CcpJsonRepresentation> getRows(String email, List<CcpJsonRepresentation> resumes) {
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		List<CcpJsonRepresentation> resumesOfTheEmail = VisBusinessSendResumesToRecruiters.getResumesOfTheEmail(resumes, rankings, this.resumeDates, this.viewsByResume);
		VisResumeRecruiterFacts storedFacts = VisBusinessSendResumesToRecruiters.loadTheFacts(email, resumesOfTheEmail);
		VisResumeRecruiterFacts facts = new VisResumeRecruiterFactsOfASavedResume(storedFacts, this.savedResumeEmailHash);
		List<CcpJsonRepresentation> rowsOfTheRecruiter = VisBusinessSendResumesToRecruiters.getRowsOfTheRecruiter(email, resumesOfTheEmail, this.lastViewByPair, this.aliasByPair, facts);
		Map<String, CcpJsonRepresentation> sortValuesById = new HashMap<>();
		for (CcpJsonRepresentation resume : resumes) {
			String resumeId = VisEntityResume.ENTITY.calculateId(resume);
			CcpJsonRepresentation sortValues = VisBusinessResumeSortValues.INSTANCE.execute(resume);
			sortValuesById.put(resumeId, sortValues);
		}
		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation rowOfTheRecruiter : rowsOfTheRecruiter) {
			String resumeId = rowOfTheRecruiter.getAsString(VisRecruiterResumesFields.resumeId);
			CcpJsonRepresentation sortValues = sortValuesById.getOrDefault(resumeId, CcpOtherConstants.EMPTY_JSON);
			CcpJsonRepresentation row = rowOfTheRecruiter
					.removeFields(VisRecruiterResumesFields.subjectItems)
					.put(VisRecruiterResumesFields.sortValues, sortValues);
			rows.add(row);
		}
		return rows;
	}

	private void sendTheEmail(CcpJsonRepresentation position, String email, List<CcpJsonRepresentation> rows) {
		String title = position.getAsString(VisEntityPosition.Fields.title);
		String positionYearMonth = position.getAsString(VisEntityPosition.Fields.yearMonth);
		CcpJsonRepresentation positionResumes = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPositionResumes.Fields.email, email)
				.put(VisEntityPositionResumes.Fields.title, title)
				.put(VisEntityPositionResumes.Fields.yearMonth, positionYearMonth)
				.put(VisEntityPositionResumes.Fields.resumes, rows);
		VisEntityPositionResumes.ENTITY.save(positionResumes);

		int total = rows.size();
		int shown = Math.min(VisBusinessSendResumesToRecruiters.RESUMES_IN_THE_EMAIL, total);
		List<CcpJsonRepresentation> rowsOfTheEmail = rows.subList(0, shown);
		CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.resumes, rowsOfTheEmail)
				.put(JnJsonCommonsFields.email, email)
				.put(VisRecruiterResumesFields.frontendUrl, this.frontendUrl)
				.put(JnJsonCommonsFields.language, VisBusinessSendResumesToRecruiters.LANGUAGE);
		CcpJsonRepresentation withTable = VisBusinessRenderResumesTable.INSTANCE.execute(tableInput);
		String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);
		String yearMonth = position.getAsString(VisEntityPosition.Fields.yearMonth);
		String seeAllUrl = this.getSeeAllUrl(email, title, yearMonth);
		String seeAll = VisBusinessSendResumesToRecruiters.getSeeAll(shown, total, seeAllUrl);

		String frequencyName = position.getOrDefault(VisEntityPosition.Fields.frequencyType, () -> VisPositionFrequencyTypes.daily.name());
		boolean instantly = VisPositionFrequencyTypes.instantly.name().equals(frequencyName);
		Class<?> template = instantly ? VisMessages.VisNotifyRecruiterAboutResumeOfThePosition.class : VisMessages.VisNotifyRecruiterAboutResumesOfThePosition.class;
		String topic = template.getName();
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(JnJsonCommonsFields.language, VisBusinessSendResumesToRecruiters.LANGUAGE)
				.put(VisRecruiterResumesFields.positionTitle, title)
				.put(VisRecruiterResumesFields.resumesTable, resumesTable)
				.put(VisRecruiterResumesFields.seeAll, seeAll)
				.put(VisRecruiterResumesFields.seeAllUrl, seeAllUrl)
				.put(VisRecruiterResumesFields.totalOfResumes, total);
		JnMessageType[] messageTypes = {JnMessageType.email};
		new JnSendMessageToUser().sendAllMessages(message, topic, messageTypes, JnMessageSenderExceptionHandler.LENIENT);
	}

	// the screen of the resumes of the position, with the recipient and the key of the position (title and month of creation)
	private String getSeeAllUrl(String email, String title, String yearMonth) {
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpUrlDecorator emailUrl = emailDecorator.url();
		String encodedEmail = emailUrl.asEnconded();
		CcpStringDecorator titleDecorator = new CcpStringDecorator(title);
		CcpUrlDecorator titleUrl = titleDecorator.url();
		String encodedTitle = titleUrl.asEnconded();
		CcpJsonRepresentation urlValues = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, encodedEmail)
				.put(VisRecruiterResumesFields.positionTitle, encodedTitle)
				.put(VisRecruiterResumesFields.positionYearMonth, yearMonth)
				.put(VisRecruiterResumesFields.frontendUrl, this.frontendUrl);
		String seeAllUrl = VisBusinessSendResumesToRecruiters.resolve(Constants.SEE_ALL_URL, urlValues);
		return seeAllUrl;
	}
}
