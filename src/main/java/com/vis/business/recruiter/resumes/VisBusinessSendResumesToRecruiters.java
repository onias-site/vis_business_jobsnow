package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.decorators.CcpUrlDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityVersionable;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSendMessageToUser;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterResumes;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityViewedResume;
import com.vis.json.transformers.VisJsonTransformerLinkedinHash;
import com.vis.messages.VisMessages;

/**
 * The daily process (Monday to Friday, as the legacy one) that sends each recruiter of {@code vis_recruiter} the table
 * of the resumes added or changed in the last 7 days (the dates come from the history, {@code jn_versionable}).
 *
 * <ul>
 * <li>Who receives: the recruiters who never viewed a resume of these e-mails and the ones whose last view is less than
 * 35 days old; who stopped viewing for longer stops receiving.</li>
 * <li>Which resumes: every resume added or changed in the last 7 days, except, for a recruiter of a professional
 * domain, the ones whose {@code notAllowedCompany} names the company of the domain.</li>
 * <li>In which order: type of resume, CLT, PJ, years of experience.</li>
 * <li>The e-mail shows the first 40; the whole list goes, before the e-mail, to {@code vis_recruiter_resumes}, read by
 * id by the screen "see all" of the front end (OQL).</li>
 * </ul>
 *
 * <p>Input: optionally {@code recruiters}, the e-mails the run is restricted to (support and tests). Output: the
 * counters of the run.
 */
public class VisBusinessSendResumesToRecruiters implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSendResumesToRecruiters INSTANCE = new VisBusinessSendResumesToRecruiters();

	private VisBusinessSendResumesToRecruiters() {}

	/** The days of resumes of each e-mail. */
	static final int DAYS_OF_RESUMES = 7;

	/** The days without a view after which the recruiter stops receiving the e-mail. */
	static final int DAYS_WITHOUT_VIEWS = 35;

	/** How many resumes the e-mail shows; the others are in the screen "see all". */
	public static final int RESUMES_IN_THE_EMAIL = 40;

	/** The shortest company name compared with a part of the domain of the recruiter. */
	static final int SHORTEST_COMPANY_NAME = 4;

	/** The language of the e-mails for now: the recruiters have no language yet. */
	static final JnLanguage LANGUAGE = JnLanguage.portuguese;

	/** The texts that are not messages. */
	static enum Constants implements CcpJsonFieldName {
		SCROLL_TIME { public String getValue() { return "1m"; } },
		SEPARATOR { public String getValue() { return "|"; } },
		NOT_LETTERS_OR_DIGITS { public String getValue() { return "[^a-z0-9]"; } },
		DOMAIN_PARTS_SEPARATORS { public String getValue() { return "[.\\-]"; } },
		LINK { public String getValue() { return "<a href=\"{frontendUrl}/meu-recrutamento/curriculos-da-semana\" target=\"_blank\">{value}</a>"; } },
		UNSUBSCRIBE_PATH { public String getValue() { return "{frontendUrl}/recrutador/descadastrar?email={email}&token={token}"; } },
	}

	/** The size of each page of the readings. */
	static final int PAGE_SIZE = 1000;

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpTimeDecorator now = new CcpTimeDecorator();
		boolean weekend = now.isWeekend();

		if(weekend) {
			CcpJsonRepresentation skipped = json.put(VisRecruiterResumesFields.weekend, true);
			return skipped;
		}

		long resumesSince = now.getDaysAgo(DAYS_OF_RESUMES);
		Set<String> changedResumes = getRecordsChangedSince(VisEntityResume.ENTITY.getEntityMetaData().entityName, resumesSince, VisEntityResume.Fields.email);
		boolean noResumes = changedResumes.isEmpty();

		if(noResumes) {
			CcpJsonRepresentation nothingToSend = json.put(VisRecruiterResumesFields.resumesFound, 0).put(VisRecruiterResumesFields.emailsSent, 0);
			return nothingToSend;
		}

		List<CcpJsonRepresentation> resumes = getResumes(changedResumes);
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		Map<String, long[]> resumeDates = getResumeDates(changedResumes);

		List<CcpJsonRepresentation> resumesOfTheEmail = new ArrayList<>();
		for (CcpJsonRepresentation resume : resumes) {
			CcpJsonRepresentation resumeOfTheEmail = getResumeOfTheEmail(resume, rankings, resumeDates);
			resumesOfTheEmail.add(resumeOfTheEmail);
		}
		resumesOfTheEmail.sort(BY_SORT_KEYS);

		long viewsSince = now.getDaysAgo(DAYS_WITHOUT_VIEWS);
		Map<String, Long> lastViewByPair = new HashMap<>();
		Map<String, Long> lastViewByRecruiter = new HashMap<>();
		loadViews(lastViewByPair, lastViewByRecruiter);
		Map<String, String> aliasByPair = getAliases();

		String frontendUrl = getFrontendUrl();
		List<String> restrictedTo = json.getOrDefault(VisRecruiterResumesFields.recruiters, () -> List.of());
		int[] emailsSent = {0};

		Consumer<CcpJsonRepresentation> sendToTheRecruiter = recruiter -> {
			String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
			boolean outOfThisRun = false == restrictedTo.isEmpty() && false == restrictedTo.contains(email);
			if(outOfThisRun) {
				return;
			}
			String emailHash = getEmailHash(email);
			Long lastViewOfTheRecruiter = lastViewByRecruiter.get(emailHash);
			boolean stoppedViewing = lastViewOfTheRecruiter != null && lastViewOfTheRecruiter < viewsSince;
			if(stoppedViewing) {
				return;
			}
			List<CcpJsonRepresentation> rows = getRowsOfTheRecruiter(recruiter, emailHash, resumesOfTheEmail, lastViewByPair, aliasByPair);
			boolean nothingForTheRecruiter = rows.isEmpty();
			if(nothingForTheRecruiter) {
				return;
			}
			this.sendTheEmail(email, rows, frontendUrl);
			emailsSent[0]++;
		};
		CcpQueryOptions everyRecruiter = CcpQueryOptions.INSTANCE.matchAll();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyRecruiter.selectFrom(VisEntityRecruiter.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, sendToTheRecruiter);

		int resumesFound = resumes.size();
		CcpJsonRepresentation counters = json
				.put(VisRecruiterResumesFields.resumesFound, resumesFound)
				.put(VisRecruiterResumesFields.emailsSent, emailsSent[0]);
		return counters;
	}

	/** Type of resume, CLT, PJ and years of experience, the keys put by {@link VisBusinessBuildResumeRow}. */
	static final Comparator<CcpJsonRepresentation> BY_SORT_KEYS = (first, second) -> {
		List<Object> firstKeys = first.getAsObjectList(VisRecruiterResumesFields.sortKeys);
		List<Object> secondKeys = second.getAsObjectList(VisRecruiterResumesFields.sortKeys);
		for (int index = 0; index < firstKeys.size(); index++) {
			double firstKey = ((Number) firstKeys.get(index)).doubleValue();
			double secondKey = ((Number) secondKeys.get(index)).doubleValue();
			int comparison = Double.compare(firstKey, secondKey);
			if(comparison != 0) {
				return comparison;
			}
		}
		return 0;
	};

	/**
	 * The row of a resume, with what the choice of the rows of each recruiter needs and the e-mail does not show: the
	 * hash of its LinkedIn (the key of the views) and the companies it forbids.
	 */
	private static CcpJsonRepresentation getResumeOfTheEmail(CcpJsonRepresentation resume, CcpJsonRepresentation rankings, Map<String, long[]> resumeDates) {
		String emailHash = resume.getAsString(VisEntityResume.Fields.email);
		long now = System.currentTimeMillis();
		long[] oldestAndNewest = resumeDates.getOrDefault(emailHash, new long[] {now, now});
		CcpJsonRepresentation rowInput = resume
				.put(VisRecruiterResumesFields.rankings, rankings)
				.put(VisRecruiterResumesFields.insertion, oldestAndNewest[0])
				.put(VisRecruiterResumesFields.lastChange, oldestAndNewest[1])
				.put(JnJsonCommonsFields.language, LANGUAGE);
		CcpJsonRepresentation row = VisBusinessBuildResumeRow.INSTANCE.execute(rowInput);
		String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
		String linkedinHash = getLinkedinHash(linkedinAddress);
		List<String> notAllowedCompanies = resume.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
		CcpJsonRepresentation resumeOfTheEmail = row
				.put(VisEntityViewedResume.Fields.linkedin, linkedinHash)
				.put(VisEntityResume.Fields.notAllowedCompany, notAllowedCompanies);
		return resumeOfTheEmail;
	}

	/**
	 * Writes the whole list of the recruiter (read by the screen "see all") and sends the e-mail with the first 40.
	 */
	private void sendTheEmail(String email, List<CcpJsonRepresentation> rows, String frontendUrl) {
		CcpJsonRepresentation recruiterResumes = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityRecruiterResumes.Fields.email, email)
				.put(VisEntityRecruiterResumes.Fields.resumes, rows);
		VisEntityRecruiterResumes.ENTITY.save(recruiterResumes);

		int total = rows.size();
		int shown = Math.min(RESUMES_IN_THE_EMAIL, total);
		List<CcpJsonRepresentation> rowsOfTheEmail = rows.subList(0, shown);
		CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.resumes, rowsOfTheEmail)
				.put(JnJsonCommonsFields.email, email)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.language, LANGUAGE);
		CcpJsonRepresentation withTable = VisBusinessRenderResumesTable.INSTANCE.execute(tableInput);
		String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);

		String seeAll = getSeeAll(shown, total, frontendUrl);
		String unsubscribeLink = getUnsubscribeLink(email, frontendUrl);

		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(JnJsonCommonsFields.language, LANGUAGE)
				.put(VisRecruiterResumesFields.resumesTable, resumesTable)
				.put(VisRecruiterResumesFields.seeAll, seeAll)
				.put(VisRecruiterResumesFields.unsubscribeLink, unsubscribeLink)
				.put(VisRecruiterResumesFields.totalOfResumes, total);
		String topic = VisMessages.VisNotifyRecruiterAboutResumes.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.email};
		new JnSendMessageToUser().sendAllMessages(message, topic, messageTypes, JnMessageSenderExceptionHandler.LENIENT);
	}

	/** The paragraph with the link "see all", empty when every resume fits in the e-mail. */
	private static String getSeeAll(int shown, int total, String frontendUrl) {
		boolean everyResumeFits = shown >= total;
		if(everyResumeFits) {
			return "";
		}
		String seeAllLabel = VisRecruiterResumesTexts.seeAll.getMessage(LANGUAGE);
		CcpJsonRepresentation linkValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(CcpJsonCommonsFields.value, seeAllLabel);
		String seeAllLink = resolve(Constants.LINK, linkValues);
		CcpJsonRepresentation paragraphValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.shown, shown)
				.put(VisRecruiterResumesFields.total, total)
				.put(VisRecruiterResumesFields.seeAllLink, seeAllLink);
		String seeAll = VisRecruiterResumesTexts.seeAllParagraph.getMessage(LANGUAGE, paragraphValues);
		return seeAll;
	}

	private static String getUnsubscribeLink(String email, String frontendUrl) {
		String token = VisRecruiterLinks.unsubscribe.sign(email);
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpUrlDecorator emailUrl = emailDecorator.url();
		String encodedEmail = emailUrl.asEnconded();
		CcpJsonRepresentation linkValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.email, encodedEmail)
				.put(VisRecruiterResumesFields.token, token);
		String unsubscribeLink = resolve(Constants.UNSUBSCRIBE_PATH, linkValues);
		return unsubscribeLink;
	}

	/**
	 * The rows of a recruiter: every resume, but the ones that forbid the company of the recruiter, each one with the
	 * last view of this recruiter, the alias they gave to it and the signed link.
	 */
	private static List<CcpJsonRepresentation> getRowsOfTheRecruiter(CcpJsonRepresentation recruiter, String emailHash, List<CcpJsonRepresentation> resumesOfTheEmail,
			Map<String, Long> lastViewByPair, Map<String, String> aliasByPair) {
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		boolean professional = recruiter.getOrDefault(VisEntityRecruiter.Fields.professional, () -> false);
		String domain = recruiter.getOrDefault(VisEntityRecruiter.Fields.domain, () -> "");
		String separator = Constants.SEPARATOR.getValue();
		String neverViewed = VisRecruiterResumesTexts.neverViewed.getMessage(LANGUAGE);
		String viewResume = VisRecruiterResumesTexts.viewResume.getMessage(LANGUAGE);

		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation resumeOfTheEmail : resumesOfTheEmail) {
			List<String> notAllowedCompanies = resumeOfTheEmail.getAsStringList(VisEntityResume.Fields.notAllowedCompany);
			boolean forbiddenCompany = professional && isForbiddenCompany(domain, notAllowedCompanies);
			if(forbiddenCompany) {
				continue;
			}
			String linkedinHash = resumeOfTheEmail.getAsString(VisEntityViewedResume.Fields.linkedin);
			String pairKey = emailHash + separator + linkedinHash;
			Long lastView = lastViewByPair.get(pairKey);
			boolean viewed = lastView != null;
			String lastViewDate = viewed ? formatDate(lastView) : neverViewed;
			CcpJsonRepresentation lastViewValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.date, lastViewDate);
			String lastViewLine = VisRecruiterResumesTexts.lastView.getMessage(LANGUAGE, lastViewValues);
			String alias = aliasByPair.get(pairKey);
			String viewLabel = alias == null ? viewResume : alias;
			String resumeId = resumeOfTheEmail.getAsString(VisRecruiterResumesFields.resumeId);
			String token = VisRecruiterLinks.view.sign(email, resumeId);
			CcpJsonRepresentation row = resumeOfTheEmail
					.removeFields(VisRecruiterResumesFields.sortKeys, VisEntityViewedResume.Fields.linkedin, VisEntityResume.Fields.notAllowedCompany)
					.put(VisRecruiterResumesFields.lastView, lastViewLine)
					.put(VisRecruiterResumesFields.viewed, viewed)
					.put(VisRecruiterResumesFields.viewLabel, viewLabel)
					.put(VisRecruiterResumesFields.token, token);
			rows.add(row);
		}
		return rows;
	}

	/**
	 * Tells whether the resume forbids the company of the recruiter: the name of a company of {@code notAllowedCompany},
	 * without accents, spaces and symbols, is a part of the domain (split by dots and hyphens) or, when it has at least
	 * four letters, is inside the domain written without dots and hyphens ({@code Itaú} forbids {@code itau.com.br} and
	 * {@code itau-unibanco.com.br}).
	 */
	static boolean isForbiddenCompany(String domain, List<String> notAllowedCompanies) {
		String lowerCaseDomain = domain.toLowerCase();
		String partsSeparators = Constants.DOMAIN_PARTS_SEPARATORS.getValue();
		String[] domainParts = lowerCaseDomain.split(partsSeparators);
		List<String> domainPartsList = List.of(domainParts);
		String joinedDomain = String.join("", domainParts);
		for (String company : notAllowedCompanies) {
			String normalizedCompany = normalize(company);
			boolean empty = normalizedCompany.isEmpty();
			if(empty) {
				continue;
			}
			boolean isAPartOfTheDomain = domainPartsList.contains(normalizedCompany);
			if(isAPartOfTheDomain) {
				return true;
			}
			boolean longEnough = normalizedCompany.length() >= SHORTEST_COMPANY_NAME;
			boolean insideTheDomain = longEnough && joinedDomain.contains(normalizedCompany);
			if(insideTheDomain) {
				return true;
			}
		}
		return false;
	}

	private static String normalize(String text) {
		CcpStringDecorator textDecorator = new CcpStringDecorator(text);
		CcpTextDecorator textText = textDecorator.text();
		CcpTextDecorator withoutAccents = textText.stripAccents();
		String lowerCase = withoutAccents.content.toLowerCase();
		String notLettersOrDigits = Constants.NOT_LETTERS_OR_DIGITS.getValue();
		String normalized = lowerCase.replaceAll(notLettersOrDigits, "");
		return normalized;
	}

	/**
	 * The primary keys of the records of an entity written since the given time, read from the history: the
	 * {@code json} of each history row (the {@code id} of the row does not come back from the query).
	 */
	private static Set<String> getRecordsChangedSince(String entityName, long since, CcpJsonFieldName keyField) {
		String timestampField = JnJsonCommonsFields.timestamp.getValue();
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.term(JnJsonCommonsFields.entity, entityName)
							.startRange()
								.startFieldRange(timestampField)
									.greaterThanEquals(since)
								.endFieldRangeAndBackToRange()
							.endRangeAndBackToMust()
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		Set<String> keys = new LinkedHashSet<>();
		Consumer<CcpJsonRepresentation> keepTheKey = historyRow -> {
			CcpJsonRepresentation record = getRecordOfTheHistoryRow(historyRow);
			String key = record.getAsString(keyField);
			keys.add(key);
		};
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String jsonField = JnJsonCommonsFields.json.getValue();
		query.selectFrom(JnEntityVersionable.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheKey, jsonField);
		return keys;
	}

	/** The stored resumes of the given e-mails (hashes); the inactive ones are not in {@code vis_resume}. */
	private static List<CcpJsonRepresentation> getResumes(Set<String> emailHashes) {
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.terms(VisEntityResume.Fields.email, emailHashes)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		List<CcpJsonRepresentation> resumes = new ArrayList<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		query.selectFrom(VisEntityResume.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, resume -> resumes.add(resume));
		return resumes;
	}

	/** The oldest (added) and the newest (last change) times of each resume in the history, by the hash of its e-mail. */
	private static Map<String, long[]> getResumeDates(Set<String> emailHashes) {
		List<String> historyIds = new ArrayList<>();
		for (String emailHash : emailHashes) {
			CcpJsonRepresentation primaryKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, emailHash);
			String historyId = primaryKey.asUgglyJson();
			historyIds.add(historyId);
		}
		String resumeEntityName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.term(JnJsonCommonsFields.entity, resumeEntityName)
							.terms(JnJsonCommonsFields.id, historyIds)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		Map<String, long[]> dates = new HashMap<>();
		Consumer<CcpJsonRepresentation> keepTheDates = historyRow -> {
			CcpJsonRepresentation record = getRecordOfTheHistoryRow(historyRow);
			String emailHash = record.getAsString(VisEntityResume.Fields.email);
			Double timestampNumber = historyRow.getAsDoubleNumber(JnJsonCommonsFields.timestamp);
			long timestamp = timestampNumber.longValue();
			long[] oldestAndNewest = dates.computeIfAbsent(emailHash, x -> new long[] {Long.MAX_VALUE, Long.MIN_VALUE});
			oldestAndNewest[0] = Math.min(oldestAndNewest[0], timestamp);
			oldestAndNewest[1] = Math.max(oldestAndNewest[1], timestamp);
		};
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String jsonField = JnJsonCommonsFields.json.getValue();
		String timestampField = JnJsonCommonsFields.timestamp.getValue();
		query.selectFrom(JnEntityVersionable.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheDates, jsonField, timestampField);
		return dates;
	}

	/**
	 * The last view of each pair recruiter + resume and of each recruiter, from the history of {@code vis_viewed_resume}
	 * (the keys are the hashes of the e-mail of the recruiter and of the LinkedIn of the resume).
	 */
	private static void loadViews(Map<String, Long> lastViewByPair, Map<String, Long> lastViewByRecruiter) {
		String viewedResumeEntityName = VisEntityViewedResume.ENTITY.getEntityMetaData().entityName;
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.term(JnJsonCommonsFields.entity, viewedResumeEntityName)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		String separator = Constants.SEPARATOR.getValue();
		Consumer<CcpJsonRepresentation> keepTheLastView = historyRow -> {
			CcpJsonRepresentation record = getRecordOfTheHistoryRow(historyRow);
			String emailHash = record.getAsString(VisEntityViewedResume.Fields.email);
			String linkedinHash = record.getAsString(VisEntityViewedResume.Fields.linkedin);
			Double timestampNumber = historyRow.getAsDoubleNumber(JnJsonCommonsFields.timestamp);
			long timestamp = timestampNumber.longValue();
			String pairKey = emailHash + separator + linkedinHash;
			lastViewByPair.merge(pairKey, timestamp, Math::max);
			lastViewByRecruiter.merge(emailHash, timestamp, Math::max);
		};
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String jsonField = JnJsonCommonsFields.json.getValue();
		String timestampField = JnJsonCommonsFields.timestamp.getValue();
		query.selectFrom(JnEntityVersionable.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheLastView, jsonField, timestampField);
	}

	/** The alias of each pair recruiter + resume, by the same key of {@link #loadViews(Map, Map)}. */
	private static Map<String, String> getAliases() {
		Map<String, String> aliasByPair = new HashMap<>();
		String separator = Constants.SEPARATOR.getValue();
		Consumer<CcpJsonRepresentation> keepTheAlias = viewedResume -> {
			String emailHash = viewedResume.getAsString(VisEntityViewedResume.Fields.email);
			String linkedinHash = viewedResume.getAsString(VisEntityViewedResume.Fields.linkedin);
			String alias = viewedResume.getAsString(VisEntityViewedResume.Fields.alias);
			String pairKey = emailHash + separator + linkedinHash;
			aliasByPair.put(pairKey, alias);
		};
		CcpQueryOptions everyView = CcpQueryOptions.INSTANCE.matchAll();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyView.selectFrom(VisEntityViewedResume.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheAlias);
		return aliasByPair;
	}

	/** The address of the front end, kept in the sending parameters of the e-mail. */
	private static String getFrontendUrl() {
		String templateId = VisMessages.VisNotifyRecruiterAboutResumes.class.getName();
		CcpJsonRepresentation parametersId = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation parameters = JnEntityEmailParametersToSend.ENTITY.getOneById(parametersId);
		CcpJsonRepresentation moreParameters = parameters.getInnerJson(JnJsonCommonsFields.moreParameters);
		String frontendUrl = moreParameters.getAsString(VisRecruiterResumesFields.frontendUrl);
		return frontendUrl;
	}

	/** The hash of the e-mail, as the entities keep it. */
	static String getEmailHash(String email) {
		CcpJsonRepresentation emailJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterResumes.Fields.email, email);
		CcpJsonRepresentation hashedEmail = VisEntityRecruiterResumes.ENTITY.getJsonWithHandledPrimaryKey(emailJson);
		String emailHash = hashedEmail.getAsString(VisEntityRecruiterResumes.Fields.email);
		return emailHash;
	}

	/** The hash of a LinkedIn address, as {@code vis_viewed_resume} keeps it. */
	static String getLinkedinHash(String linkedinAddress) {
		CcpJsonRepresentation linkedinJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress);
		CcpJsonRepresentation hashedLinkedin = new VisJsonTransformerLinkedinHash().execute(linkedinJson);
		String linkedinHash = hashedLinkedin.getAsString(VisEntityViewedResume.Fields.linkedin);
		return linkedinHash;
	}

	private static CcpJsonRepresentation getRecordOfTheHistoryRow(CcpJsonRepresentation historyRow) {
		String recordAsText = historyRow.getAsString(JnJsonCommonsFields.json);
		CcpJsonRepresentation record = new CcpJsonRepresentation(recordAsText);
		return record;
	}

	static String formatDate(long time) {
		CcpTimeDecorator timeDecorator = new CcpTimeDecorator(time);
		String datePattern = VisBusinessBuildResumeRow.Constants.DATE_PATTERN.getValue();
		String formattedDate = timeDecorator.getFormattedDateTime(datePattern);
		return formattedDate;
	}

	private static String resolve(CcpJsonFieldName template, CcpJsonRepresentation values) {
		String templateText = template.getValue();
		CcpStringDecorator templateDecorator = new CcpStringDecorator(templateText);
		CcpTextDecorator templateTextDecorator = templateDecorator.text();
		CcpTextDecorator resolved = templateTextDecorator.resolveTemplate(values);
		return resolved.content;
	}
}
